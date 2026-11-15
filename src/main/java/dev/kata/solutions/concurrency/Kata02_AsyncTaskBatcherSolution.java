package dev.kata.solutions.concurrency;

import dev.kata.concurrency.Kata02_AsyncTaskBatcher;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Эталонное решение Kata 02.
 *
 * <h2>Модель состояния</h2>
 *
 * <p>Всё состояние батчера — очередь {@code pending}, флаг {@code closed} и ссылка на
 * запланированный таймер — защищено одним {@link ReentrantLock}. Блокировка удерживается только на
 * время манипуляций с очередью (десятки наносекунд), но <b>никогда</b> во время вызова {@code
 * batchFunction}. Это даёт простую и строгую инвариантность:
 *
 * <ul>
 *   <li>{@code pending.size() <= maxBatchSize} в любой момент;
 *   <li>каждый элемент ровно один раз попадает в пачку — либо по размеру, либо по таймеру, либо в
 *       финальный flush при {@code close};
 *   <li>таймер существует тогда и только тогда, когда очередь непуста и пачка ещё не достигла
 *       {@code maxBatchSize}.
 * </ul>
 *
 * <h2>Почему таймер планируется только на переходе «пусто → непусто»</h2>
 *
 * <p>Если планировать задачу на каждый {@code submit}, окно {@code maxWaitTime} будет постоянно
 * сдвигаться вправо, и пачка из редких запросов может ждать бесконечно. Поэтому таймер ставится
 * только когда в пустую очередь добавляется первый элемент. При отправке пачки по размеру таймер
 * отменяется.
 *
 * <h2>Почему drain — это «забрать всё»</h2>
 *
 * <p>Инвариант «пачка никогда не больше maxBatchSize» поддерживается тем, что каждый {@code submit}
 * проверяет размер под блокировкой и при достижении порога немедленно забирает пачку. Пока
 * блокировка удерживается, никто не может добавить элемент сверх порога. Поэтому таймерный и
 * финальный flush тоже просто забирают всё накопленное — это заведомо не больше {@code
 * maxBatchSize}.
 *
 * <h2>Поток исполнения</h2>
 *
 * <p>{@code submit} не блокируется: он лишь добавляет элемент и, возможно, отправляет задачу в
 * переданный executor. Сам {@code batchFunction} выполняется на этом executor. Внутренний {@link
 * ScheduledExecutorService} — один демон-поток, который только «будит» батчер по таймауту;
 * пользовательский код на нём не исполняется.
 *
 * <h2>Ошибки</h2>
 *
 * <p>Любое исключение из {@code batchFunction} (включая {@link Error}) завершает все future пачки
 * исключительно, но не роняет поток executor. Если результат имеет неверный размер, future
 * завершаются {@link IllegalStateException}: молча раздавать «сдвинутые» результаты опаснее, чем
 * явно упасть.
 */
public final class Kata02_AsyncTaskBatcherSolution<T, R> extends Kata02_AsyncTaskBatcher<T, R> {

  private final ReentrantLock lock = new ReentrantLock();
  private final Deque<Pending<T, R>> pending = new ArrayDeque<>();
  private final ScheduledExecutorService timer;

  /** Текущая запланированная задача таймаута; {@code null}, если очередь пуста. */
  private ScheduledFuture<?> timeoutTask;

  private boolean closed;

  public Kata02_AsyncTaskBatcherSolution(
      int maxBatchSize,
      Duration maxWaitTime,
      Function<List<T>, List<R>> batchFunction,
      Executor executor) {
    super(maxBatchSize, maxWaitTime, batchFunction, executor);
    if (maxBatchSize <= 0) {
      throw new IllegalArgumentException("maxBatchSize must be > 0, got " + maxBatchSize);
    }
    Objects.requireNonNull(maxWaitTime, "maxWaitTime");
    if (maxWaitTime.isZero() || maxWaitTime.isNegative()) {
      throw new IllegalArgumentException("maxWaitTime must be > 0, got " + maxWaitTime);
    }
    Objects.requireNonNull(batchFunction, "batchFunction");
    Objects.requireNonNull(executor, "executor");
    this.timer =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "kata02-batcher-timer");
              thread.setDaemon(true);
              return thread;
            });
  }

  @Override
  public CompletableFuture<R> submit(T item) {
    Objects.requireNonNull(item, "item");

    Pending<T, R> entry = new Pending<>(item);
    List<Pending<T, R>> batch = null;

    lock.lock();
    try {
      if (closed) {
        return CompletableFuture.failedFuture(new IllegalStateException("batcher is closed"));
      }

      boolean wasEmpty = pending.isEmpty();
      pending.addLast(entry);

      if (pending.size() >= maxBatchSize) {
        batch = drainAllLocked();
        cancelTimeoutLocked();
      } else if (wasEmpty) {
        scheduleTimeoutLocked();
      }
    } finally {
      lock.unlock();
    }

    if (batch != null) {
      dispatch(batch);
    }
    return entry.future;
  }

  @Override
  public void close() {
    List<Pending<T, R>> batch = null;

    lock.lock();
    try {
      if (closed) {
        return;
      }
      closed = true;
      cancelTimeoutLocked();
      if (!pending.isEmpty()) {
        batch = drainAllLocked();
      }
    } finally {
      lock.unlock();
    }

    // Отправляем финальную пачку, чтобы ожидающие future не повисли.
    if (batch != null) {
      dispatch(batch);
    }
    timer.shutdown();
  }

  /** Забирает всю очередь. Вызывать только под {@link #lock}. */
  private List<Pending<T, R>> drainAllLocked() {
    List<Pending<T, R>> batch = new ArrayList<>(pending);
    pending.clear();
    return batch;
  }

  /** Вызывать только под {@link #lock}. */
  private void scheduleTimeoutLocked() {
    long delayNanos = maxWaitTime.toNanos();
    timeoutTask = timer.schedule(this::flushOnTimeout, delayNanos, TimeUnit.NANOSECONDS);
  }

  /** Вызывать только под {@link #lock}. */
  private void cancelTimeoutLocked() {
    if (timeoutTask != null) {
      timeoutTask.cancel(false);
      timeoutTask = null;
    }
  }

  /**
   * Срабатывание таймера. Задача могла «опоздать»: пачку уже отправили по размеру или закрыли
   * батчер. Тогда очередь пуста и метод просто выходит.
   */
  private void flushOnTimeout() {
    List<Pending<T, R>> batch = null;

    lock.lock();
    try {
      timeoutTask = null;
      if (!pending.isEmpty()) {
        batch = drainAllLocked();
      }
    } finally {
      lock.unlock();
    }

    if (batch != null) {
      dispatch(batch);
    }
  }

  private void dispatch(List<Pending<T, R>> batch) {
    try {
      executor.execute(() -> runBatch(batch));
    } catch (RejectedExecutionException e) {
      failAll(batch, e);
    }
  }

  private void runBatch(List<Pending<T, R>> batch) {
    List<T> items = new ArrayList<>(batch.size());
    for (Pending<T, R> entry : batch) {
      items.add(entry.item);
    }

    List<R> results;
    try {
      results = batchFunction.apply(List.copyOf(items));
    } catch (Throwable t) {
      // Не пробрасываем дальше: иначе исключение потеряется в пуле executor,
      // а вызывающие останутся с вечно незавершёнными future.
      failAll(batch, t);
      return;
    }

    if (results == null || results.size() != batch.size()) {
      failAll(
          batch,
          new IllegalStateException(
              "batchFunction must return exactly "
                  + batch.size()
                  + " results, got "
                  + (results == null ? "null" : results.size())));
      return;
    }

    for (int i = 0; i < batch.size(); i++) {
      batch.get(i).future.complete(results.get(i));
    }
  }

  private static <T, R> void failAll(List<Pending<T, R>> batch, Throwable error) {
    for (Pending<T, R> entry : batch) {
      entry.future.completeExceptionally(error);
    }
  }

  /** Пара «элемент + его future». Иммутабельна после создания. */
  private static final class Pending<T, R> {
    private final T item;
    private final CompletableFuture<R> future = new CompletableFuture<>();

    private Pending(T item) {
      this.item = item;
    }
  }
}
