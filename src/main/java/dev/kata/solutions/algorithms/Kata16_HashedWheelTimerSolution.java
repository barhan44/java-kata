package dev.kata.solutions.algorithms;

import dev.kata.algorithms.Kata16_HashedWheelTimer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Эталонное решение Kata 16.
 *
 * <h2>Индекс бакета и обороты</h2>
 *
 * <p>Дедлайн переводится в номер тика: {@code T = ceil((elapsed + delay) / tickDuration)}.
 * Округление вверх гарантирует, что задача не сработает раньше срока. Пусть {@code c} — последний
 * обработанный тик. Задача кладётся в бакет {@code T & (ticksPerWheel - 1)} с числом полных
 * оборотов {@code rounds = (T - c - 1) / ticksPerWheel}. На ближайшем визите бакета (тик {@code t
 * >= c + 1}) {@code rounds} уменьшается; когда он равен нулю, задача срабатывает. Формула с {@code
 * -1} компенсирует то, что бакет {@code T} может быть посещён уже на следующем тике, если {@code T}
 * совпадает с текущей позицией колеса.
 *
 * <h2>Синхронизация</h2>
 *
 * <p>Один {@link ReentrantLock} защищает кольцо, списки, {@code tick}, {@code startNanos} и флаги
 * состояния. Критические секции короткие: вставка/удаление из двусвязного списка, O(1). Задачи
 * выполняются <b>вне</b> блокировки: тикер под lock только собирает сработавшие таймеры в
 * переиспользуемый {@code dueBuffer}, затем отпускает lock и запускает их. Иначе медленная задача
 * остановила бы всё планирование.
 *
 * <p>Альтернатива Netty — отдельная блокировка на бакет и lock-free очередь новых таймеров. Это
 * даёт большую пропускную способность при сотнях потоков-планировщиков, но заметно усложняет
 * доказательство корректности гонки «планирование vs тик». Здесь выбран глобальный lock: O(1) на
 * операцию сохраняется, а поведение проще проверить.
 *
 * <h2>Отмена и остановка</h2>
 *
 * <p>{@link Timeout#cancel()} сначала делает CAS {@code PENDING -> CANCELLED}. Проигравший (таймер
 * уже сработал) возвращает {@code false}. Победитель затем берёт lock и удаляет узел из списка.
 * Если тикер успел увидеть {@code CANCELLED} раньше, он удалит узел сам; повторное удаление
 * идемпотентно. {@code stop} под lock переводит состояние в {@code STOPPED}, отменяет все узлы в
 * бакетах, затем будит и join-ит тикер (кроме случая вызова из самой задачи тикера).
 *
 * <h2>Догоняющий тикер</h2>
 *
 * <p>Тикер спит до абсолютного времени следующего тика ({@code startNanos + (tick+1)*tickNanos}),
 * поэтому не накапливает дрейф. После пробуждения он вычисляет фактический номер тика по {@link
 * System#nanoTime()} и обрабатывает все пропущенные тики по порядку. Если поток был приостановлен
 * ОС на секунду, задачи не «потеряются» — они сработают с опозданием, но в правильном порядке.
 * Раннее/ложное пробуждение игнорируется: пока {@code elapsedTick <= tick}, тик не продвигается.
 *
 * <h2>Аллокации</h2>
 *
 * <p>Один {@link Timeout} на {@code schedule}. {@code dueBuffer} переиспользуется между тиками;
 * {@code cancel}/{@code stop} не создают объектов. {@code schedule} после остановки бросает
 * исключение — это единственный «дорогой» путь.
 */
public final class Kata16_HashedWheelTimerSolution extends Kata16_HashedWheelTimer {

  private static final int STATE_NEW = 0;
  private static final int STATE_RUNNING = 1;
  private static final int STATE_STOPPED = 2;

  private static final int PENDING = 0;
  private static final int CANCELLED = 1;
  private static final int EXPIRED = 2;

  private final ReentrantLock lock = new ReentrantLock();
  private final Bucket[] buckets;
  private final int mask;
  private final long tickNanos;

  /** Собирает сработавшие таймеры; доступен только тикеру. */
  private final List<Timeout> dueBuffer = new ArrayList<>();

  private volatile int state = STATE_NEW;
  private long startNanos;
  private long tick;
  private Thread tickerThread;

  public Kata16_HashedWheelTimerSolution(int ticksPerWheel, Duration tickDuration) {
    super(ticksPerWheel, tickDuration);
    if (ticksPerWheel <= 0 || (ticksPerWheel & (ticksPerWheel - 1)) != 0) {
      throw new IllegalArgumentException(
          "ticksPerWheel must be a positive power of two, got " + ticksPerWheel);
    }
    Objects.requireNonNull(tickDuration, "tickDuration");
    if (tickDuration.isZero() || tickDuration.isNegative()) {
      throw new IllegalArgumentException("tickDuration must be > 0, got " + tickDuration);
    }
    this.tickNanos = toNanosSaturated(tickDuration);
    if (tickNanos <= 0L) {
      throw new IllegalArgumentException("tickDuration is too small: " + tickDuration);
    }
    this.mask = ticksPerWheel - 1;
    this.buckets = new Bucket[ticksPerWheel];
    for (int i = 0; i < ticksPerWheel; i++) {
      buckets[i] = new Bucket();
    }
  }

  @Override
  public void start() {
    lock.lock();
    try {
      if (state == STATE_RUNNING) {
        return;
      }
      if (state == STATE_STOPPED) {
        throw new IllegalStateException("timer is stopped");
      }
      startNanos = System.nanoTime();
      tick = 0L;
      state = STATE_RUNNING;
      tickerThread =
          Thread.ofPlatform().daemon().name("kata16-hashed-wheel").unstarted(this::runTicker);
      tickerThread.start();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public TimeoutHandle schedule(Runnable task, Duration delay) {
    Objects.requireNonNull(task, "task");
    Objects.requireNonNull(delay, "delay");
    if (delay.isNegative()) {
      throw new IllegalArgumentException("delay must be >= 0, got " + delay);
    }
    long delayNanos = toNanosSaturated(delay);

    lock.lock();
    try {
      if (state != STATE_RUNNING) {
        throw new IllegalStateException("timer is not running");
      }

      long elapsedNanos = System.nanoTime() - startNanos;
      long deadlineOffset = saturatedAdd(elapsedNanos, delayNanos);
      long deadlineTick = ceilDiv(deadlineOffset, tickNanos);

      long current = tick;
      long calculated = Math.max(deadlineTick, current + 1L);
      int bucketIndex = (int) (calculated & mask);
      long rounds = (calculated - current - 1L) / ticksPerWheel;

      Timeout timeout = new Timeout(task, buckets[bucketIndex]);
      timeout.remainingRounds = rounds;
      buckets[bucketIndex].addLast(timeout);
      return timeout;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void stop() {
    Thread toJoin;
    lock.lock();
    try {
      if (state == STATE_STOPPED) {
        return;
      }
      state = STATE_STOPPED;
      cancelAllLocked();
      toJoin = tickerThread;
    } finally {
      lock.unlock();
    }

    if (toJoin != null && toJoin != Thread.currentThread()) {
      toJoin.interrupt();
      try {
        toJoin.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void close() {
    stop();
  }

  private void runTicker() {
    while (state == STATE_RUNNING) {
      long targetTick;
      lock.lock();
      try {
        if (state != STATE_RUNNING) {
          return;
        }
        targetTick = tick + 1L;
      } finally {
        lock.unlock();
      }

      long targetNanos = startNanos + targetTick * tickNanos;
      long sleepNanos = targetNanos - System.nanoTime();
      if (sleepNanos > 0L) {
        LockSupport.parkNanos(sleepNanos);
        if (Thread.interrupted() && state != STATE_RUNNING) {
          return;
        }
      }
      if (state != STATE_RUNNING) {
        return;
      }

      lock.lock();
      try {
        if (state != STATE_RUNNING) {
          return;
        }
        long elapsedTick = (System.nanoTime() - startNanos) / tickNanos;
        if (elapsedTick <= tick) {
          // Раннее или ложное пробуждение: следующий тик ещё не наступил.
          continue;
        }
        for (long next = tick + 1L; next <= elapsedTick; next++) {
          tick = next;
          processBucket((int) (next & mask));
        }
      } finally {
        lock.unlock();
      }
      runDue();
    }
  }

  /** Вызывается под lock. Собирает сработавшие таймеры в {@link #dueBuffer}. */
  private void processBucket(int bucketIndex) {
    Bucket bucket = buckets[bucketIndex];
    Timeout timeout = bucket.head;
    while (timeout != null) {
      Timeout next = timeout.next;
      int currentState = timeout.state.get();
      if (currentState == CANCELLED || currentState == EXPIRED) {
        bucket.remove(timeout);
        timeout.bucket = null;
      } else if (timeout.remainingRounds > 0L) {
        timeout.remainingRounds--;
      } else if (timeout.state.compareAndSet(PENDING, EXPIRED)) {
        bucket.remove(timeout);
        timeout.bucket = null;
        dueBuffer.add(timeout);
      } else {
        // cancel успел выиграть CAS, пока мы держали lock: убираем узел.
        bucket.remove(timeout);
        timeout.bucket = null;
      }
      timeout = next;
    }
  }

  /** Запускает собранные задачи вне блокировки; ошибка одной задачи не убивает тикер. */
  private void runDue() {
    if (dueBuffer.isEmpty()) {
      return;
    }
    for (int i = 0; i < dueBuffer.size(); i++) {
      try {
        dueBuffer.get(i).task.run();
      } catch (Throwable ignored) {
        // Контракт: сбой задачи не должен останавливать остальные таймеры.
      }
    }
    dueBuffer.clear();
  }

  /** Вызывается под lock. */
  private void cancelAllLocked() {
    for (Bucket bucket : buckets) {
      Timeout timeout = bucket.head;
      while (timeout != null) {
        Timeout next = timeout.next;
        timeout.state.compareAndSet(PENDING, CANCELLED);
        timeout.bucket = null;
        timeout.prev = null;
        timeout.next = null;
        timeout = next;
      }
      bucket.clear();
    }
  }

  private static long toNanosSaturated(Duration duration) {
    try {
      return duration.toNanos();
    } catch (ArithmeticException overflow) {
      return Long.MAX_VALUE;
    }
  }

  private static long saturatedAdd(long left, long right) {
    long sum = left + right;
    return sum < 0L ? Long.MAX_VALUE : sum;
  }

  private static long ceilDiv(long value, long divisor) {
    long quotient = value / divisor;
    return value % divisor == 0L ? quotient : quotient + 1L;
  }

  /** Узел двусвязного списка бакета. */
  private final class Timeout implements TimeoutHandle {
    private final Runnable task;
    private final AtomicInteger state = new AtomicInteger(PENDING);
    private Bucket bucket;
    private long remainingRounds;
    private Timeout prev;
    private Timeout next;

    Timeout(Runnable task, Bucket bucket) {
      this.task = task;
      this.bucket = bucket;
    }

    @Override
    public boolean cancel() {
      if (!state.compareAndSet(PENDING, CANCELLED)) {
        return false;
      }
      lock.lock();
      try {
        if (bucket != null) {
          bucket.remove(this);
          bucket = null;
        }
      } finally {
        lock.unlock();
      }
      return true;
    }

    @Override
    public boolean isExpired() {
      return state.get() == EXPIRED;
    }
  }

  /**
   * Бакет кольца: двусвязный список. Все операции — под общим lock таймера.
   *
   * <p>Двусвязность нужна для O(1)-отмены: по одному только {@code next} удаление требовало бы
   * поиска предыдущего узла.
   */
  private static final class Bucket {
    private Timeout head;
    private Timeout tail;

    void addLast(Timeout timeout) {
      timeout.prev = tail;
      timeout.next = null;
      if (tail == null) {
        head = timeout;
      } else {
        tail.next = timeout;
      }
      tail = timeout;
      timeout.bucket = this;
    }

    void remove(Timeout timeout) {
      Timeout previous = timeout.prev;
      Timeout next = timeout.next;
      if (previous == null) {
        head = next;
      } else {
        previous.next = next;
      }
      if (next == null) {
        tail = previous;
      } else {
        next.prev = previous;
      }
      timeout.prev = null;
      timeout.next = null;
    }

    void clear() {
      head = null;
      tail = null;
    }
  }
}
