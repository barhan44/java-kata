package dev.kata.solutions.patterns;

import dev.kata.patterns.Kata11_CircuitBreaker;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

/**
 * Эталонное решение Kata 11.
 *
 * <h2>Границы блокировки</h2>
 *
 * <p>Состояние автомата, кольцо результатов и счётчики {@code HALF_OPEN} защищены одним {@link
 * ReentrantLock}. Под блокировкой выполняются только проверки и O(1)-обновления. Пользовательский
 * {@code Callable} вызывается <b>вне</b> блокировки: иначе один медленный запрос заблокировал бы
 * все остальные потоки, включая те, которые должны получить быстрый отказ.
 *
 * <p>Это компромисс: один глобальный lock — точка конкуренции. Для circuit breaker это приемлемо:
 * критическая секция измеряется десятками наносекунд, а вызов удалённого сервиса — миллисекундами.
 * Если счётчик отказов станет горячим, его можно вынести в {@code LongAdder}, но переходы состояния
 * всё равно требуют сериализации.
 *
 * <h2>Эпохи (epoch) и «поздние» результаты</h2>
 *
 * <p>Вызов, допущенный в {@code CLOSED}, может завершиться уже после того, как другой вызов открыл
 * предохранитель. Если записать его результат в кольцо нового поколения, можно преждевременно
 * закрыть или снова открыть автомат. Поэтому каждый допуск запоминает {@code epoch}, а запись
 * результата проверяет, что эпоха не изменилась. Любой переход состояния увеличивает {@code epoch}:
 *
 * <ul>
 *   <li>CLOSED → OPEN — результаты старых вызовов больше не учитываются;
 *   <li>OPEN → HALF_OPEN — старые CLOSED-вызовы не должны попасть в пробную статистику;
 *   <li>HALF_OPEN → CLOSED/OPEN — «запоздавшие» пробы не должны менять решение.
 * </ul>
 *
 * <h2>Семантика HALF_OPEN</h2>
 *
 * <p>Разрешение выдаётся под блокировкой: счётчик {@code halfOpenPermitsLeft} уменьшается, поэтому
 * при 100 одновременных вызовах ровно {@code permittedCallsInHalfOpen} дойдут до функции, а
 * остальные получат {@link CircuitBreakerOpenException}. Успешные пробы копят {@code
 * halfOpenSuccesses}; когда их набралось {@code permittedCallsInHalfOpen}, автомат закрывается.
 * Любая неудача немедленно открывает его снова и сбрасывает {@code openedAt}, то есть полный {@code
 * openDuration} начинается заново.
 *
 * <p>Если проб успело прийти меньше, чем разрешено, автомат остаётся в {@code HALF_OPEN} и
 * продолжает пропускать оставшиеся пробы. Это осознанно: закрываться после одной успешной пробы,
 * когда разрешено пять, — значит не собрать достаточной статистики.
 *
 * <h2>Кольцевой буфер</h2>
 *
 * <p>{@code boolean[]} фиксированного размера; {@code index} указывает на самую старую ячейку после
 * заполнения. При перезаписи старый результат вычитается из {@code failureCount}, поэтому доля
 * ошибок считается за O(1), без потоков и без обхода окна. {@code minimumCalls} не даёт открыть
 * предохранитель после одного-двух случайных отказов на старте.
 *
 * <h2>Время</h2>
 *
 * <p>{@link LongSupplier} инжектируется, поэтому тесты двигают виртуальные часы и не спят.
 * Сравнение {@code now - openedAt >= duration} корректно переживает переполнение {@code long} и
 * «прыжок назад»: отрицательная разность трактуется как «время ещё не прошло».
 */
public final class Kata11_CircuitBreakerSolution extends Kata11_CircuitBreaker {

  private final ReentrantLock lock = new ReentrantLock();
  private final SlidingWindow window;
  private final long openDurationNanos;

  private State state = State.CLOSED;
  private long openedAtNanos;
  private long epoch;
  private int halfOpenPermitsLeft;
  private int halfOpenSuccesses;
  private long rejectedCalls;

  public Kata11_CircuitBreakerSolution(
      int slidingWindowSize,
      int minimumCalls,
      double failureRateThreshold,
      Duration openDuration,
      int permittedCallsInHalfOpen) {
    this(
        slidingWindowSize,
        minimumCalls,
        failureRateThreshold,
        openDuration,
        permittedCallsInHalfOpen,
        System::nanoTime);
  }

  public Kata11_CircuitBreakerSolution(
      int slidingWindowSize,
      int minimumCalls,
      double failureRateThreshold,
      Duration openDuration,
      int permittedCallsInHalfOpen,
      LongSupplier nanoTimeSource) {
    super(
        slidingWindowSize,
        minimumCalls,
        failureRateThreshold,
        openDuration,
        permittedCallsInHalfOpen,
        nanoTimeSource);
    if (slidingWindowSize <= 0) {
      throw new IllegalArgumentException("slidingWindowSize must be > 0, got " + slidingWindowSize);
    }
    if (minimumCalls <= 0 || minimumCalls > slidingWindowSize) {
      throw new IllegalArgumentException(
          "minimumCalls must be in [1, slidingWindowSize], got " + minimumCalls);
    }
    if (!(failureRateThreshold > 0.0) || failureRateThreshold > 1.0) {
      throw new IllegalArgumentException(
          "failureRateThreshold must be in (0, 1], got " + failureRateThreshold);
    }
    Objects.requireNonNull(openDuration, "openDuration");
    if (openDuration.isZero() || openDuration.isNegative()) {
      throw new IllegalArgumentException("openDuration must be > 0, got " + openDuration);
    }
    if (permittedCallsInHalfOpen <= 0) {
      throw new IllegalArgumentException(
          "permittedCallsInHalfOpen must be > 0, got " + permittedCallsInHalfOpen);
    }
    Objects.requireNonNull(nanoTimeSource, "nanoTimeSource");

    this.window = new SlidingWindow(slidingWindowSize);
    this.openDurationNanos = toNanosSaturated(openDuration);
  }

  @Override
  public <T> T execute(Callable<T> call) throws Exception {
    Objects.requireNonNull(call, "call");
    Admission admission = admit();
    try {
      T result = call.call();
      record(admission, true);
      return result;
    } catch (Exception e) {
      record(admission, false);
      throw e;
    } catch (Error e) {
      record(admission, false);
      throw e;
    }
  }

  @Override
  public State currentState() {
    lock.lock();
    try {
      maybeHalfOpen(nanoTimeSource.getAsLong());
      return state;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long rejectedCalls() {
    lock.lock();
    try {
      return rejectedCalls;
    } finally {
      lock.unlock();
    }
  }

  /** Проверяет допуск и при необходимости выполняет переход OPEN → HALF_OPEN. */
  private Admission admit() {
    lock.lock();
    try {
      long now = nanoTimeSource.getAsLong();
      maybeHalfOpen(now);

      if (state == State.CLOSED) {
        return new Admission(Kind.CLOSED, epoch);
      }

      // state == HALF_OPEN (OPEN либо перешёл в HALF_OPEN, либо отклонится ниже).
      if (state == State.OPEN) {
        rejectedCalls++;
        throw new CircuitBreakerOpenException(State.OPEN);
      }
      if (halfOpenPermitsLeft > 0) {
        halfOpenPermitsLeft--;
        return new Admission(Kind.HALF_OPEN, epoch);
      }
      rejectedCalls++;
      throw new CircuitBreakerOpenException(State.HALF_OPEN);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Ленивый переход OPEN → HALF_OPEN. Вызывается только под {@link #lock}.
   *
   * <p>Отдельный {@code if} после {@code maybeHalfOpen} в {@link #admit()} нужен, чтобы отклонить
   * вызов, если {@code openDuration} ещё не истёк: {@code maybeHalfOpen} в этом случае оставляет
   * состояние {@code OPEN}.
   */
  private void maybeHalfOpen(long now) {
    if (state == State.OPEN && now - openedAtNanos >= openDurationNanos) {
      state = State.HALF_OPEN;
      halfOpenPermitsLeft = permittedCallsInHalfOpen;
      halfOpenSuccesses = 0;
      epoch++;
    }
  }

  /** Записывает результат допущенного вызова; устаревшие эпохи игнорируются. */
  private void record(Admission admission, boolean success) {
    lock.lock();
    try {
      if (admission.epoch != epoch) {
        return;
      }
      if (admission.kind == Kind.CLOSED) {
        if (state != State.CLOSED) {
          return;
        }
        window.record(!success);
        if (window.shouldTrip(minimumCalls, failureRateThreshold)) {
          tripOpen();
        }
      } else {
        if (state != State.HALF_OPEN) {
          return;
        }
        if (!success) {
          tripOpen();
        } else {
          halfOpenSuccesses++;
          if (halfOpenSuccesses >= permittedCallsInHalfOpen) {
            closeBreaker();
          }
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /** OPEN с чистым окном: после восстановления статистика должна набираться заново. */
  private void tripOpen() {
    state = State.OPEN;
    openedAtNanos = nanoTimeSource.getAsLong();
    window.reset();
    halfOpenPermitsLeft = 0;
    halfOpenSuccesses = 0;
    epoch++;
  }

  private void closeBreaker() {
    state = State.CLOSED;
    window.reset();
    halfOpenPermitsLeft = 0;
    halfOpenSuccesses = 0;
    epoch++;
  }

  private static long toNanosSaturated(Duration duration) {
    try {
      return duration.toNanos();
    } catch (ArithmeticException overflow) {
      return Long.MAX_VALUE;
    }
  }

  private enum Kind {
    CLOSED,
    HALF_OPEN
  }

  private record Admission(Kind kind, long epoch) {}

  /** Кольцевой буфер результатов последних вызовов. Все методы — под блокировкой автомата. */
  private static final class SlidingWindow {
    private final boolean[] failures;
    private int index;
    private int size;
    private int failureCount;

    SlidingWindow(int capacity) {
      this.failures = new boolean[capacity];
    }

    void record(boolean failure) {
      if (size == failures.length) {
        if (failures[index]) {
          failureCount--;
        }
      } else {
        size++;
      }
      failures[index] = failure;
      if (failure) {
        failureCount++;
      }
      index++;
      if (index == failures.length) {
        index = 0;
      }
    }

    boolean shouldTrip(int minimumCalls, double threshold) {
      return size >= minimumCalls && (double) failureCount / size > threshold;
    }

    void reset() {
      Arrays.fill(failures, false);
      index = 0;
      size = 0;
      failureCount = 0;
    }
  }
}
