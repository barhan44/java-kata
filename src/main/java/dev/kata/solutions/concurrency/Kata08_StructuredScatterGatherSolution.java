package dev.kata.solutions.concurrency;

import dev.kata.concurrency.Kata08_StructuredScatterGather;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Эталонное решение Kata 08.
 *
 * <h2>Почему виртуальные потоки, а не {@code StructuredTaskScope}</h2>
 *
 * <p>JDK 21 предоставляет {@code StructuredTaskScope} в preview-режиме. Его встроенная стратегия
 * {@code ShutdownOnSuccess} умеет ровно то, что нужно на финише: первый успех завершает scope и
 * прерывает остальных. Но hedged request требует <b>динамического добавления</b> задач после
 * таймера, а {@code ShutdownOnSuccess} — финальный класс, и комбинировать «подождать hedgeDelay,
 * затем форкнуть ещё одну задачу, но не ждать её, если первая уже ответила» на нём неудобно.
 * Поэтому здесь используется явный {@link Executors#newVirtualThreadPerTaskExecutor()}: виртуальный
 * поток на попытку стоит копейки, а логика хеджирования остаётся прозрачной.
 *
 * <h2>Архитектура</h2>
 *
 * <ul>
 *   <li><b>Очередь завершений.</b> Каждая попытка кладёт в {@link LinkedBlockingQueue} ровно один
 *       результат: {@link Attempt#success} или {@link Attempt#failure}. Главный поток делает {@code
 *       take()} и реагирует на первое событие — не нужно опрашивать futures и не нужно {@code
 *       CompletableFuture.anyOf}, который не умеет «первый успех, игнорируя ошибки».
 *   <li><b>Победитель определяется CAS-ом.</b> Попытка, завершившаяся успешно, делает {@code
 *       finished.compareAndSet(false, true)}. Только победитель кладёт success в очередь. Это
 *       исключает зависимость результата от планировщика при одновременном успехе двух реплик.
 *   <li><b>Хедж-таймер.</b> Один daemon-поток планирует запуск следующей реплики через {@code
 *       hedgeDelay}. И таймер, и обработчик ошибок пытаются запустить следующую реплику; ровно один
 *       выигрывает {@code launched.compareAndSet(nextIndex, nextIndex + 1)}. Так исключается
 *       двойной запуск.
 *   <li><b>Агрегация ошибок.</b> Ошибки складываются в массив по индексу реплики, а не в порядке
 *       завершения. Тест и стектрейс получают детерминированный порядок.
 * </ul>
 *
 * <h2>Отмена и прерывание</h2>
 *
 * <p>После победы {@link AttemptTracker#cancelAll()} под своей блокировкой помечает трекер
 * отменённым и вызывает {@code cancel(true)} для всех futures. Та же блокировка используется при
 * регистрации новой попытки, поэтому невозможна гонка «cancelAll прошёл по списку, а таймер уже
 * добавил ещё одну задачу после него». Затем executor получает {@code shutdownNow()} — виртуальные
 * потоки просыпаются с {@link InterruptedException}.
 *
 * <p>Отмена кооперативна: если пользовательский {@code Callable} игнорирует прерывание, {@code
 * execute} подождёт его до 5 секунд и вернёт результат победителя, не блокируясь навсегда. Это
 * осознанный компромисс: нельзя безопасно «убить» поток в Java.
 *
 * <h2>Аллокации</h2>
 *
 * <p>На попытку: один виртуальный поток, один {@link Future} в трекере и одна запись {@link
 * Attempt}. Список реплик не копируется, {@code Callable} не оборачивается в лишние декораторы.
 * {@link LinkedBlockingQueue} выбран вместо {@code ArrayBlockingQueue} без ёмкости: число попыток
 * заранее неизвестно, а очередь ограничена числом реплик.
 */
public final class Kata08_StructuredScatterGatherSolution extends Kata08_StructuredScatterGather {

  /** Сколько ждать управляемого завершения отменённых виртуальных потоков. */
  private static final long CANCEL_AWAIT_MILLIS = 5_000L;

  private final long hedgeDelayNanos;

  public Kata08_StructuredScatterGatherSolution(Duration hedgeDelay) {
    super(hedgeDelay);
    Objects.requireNonNull(hedgeDelay, "hedgeDelay");
    if (hedgeDelay.isZero() || hedgeDelay.isNegative()) {
      throw new IllegalArgumentException("hedgeDelay must be > 0, got " + hedgeDelay);
    }
    this.hedgeDelayNanos = toNanosSaturated(hedgeDelay);
  }

  @Override
  public <T> T execute(List<? extends Callable<T>> replicas) {
    Objects.requireNonNull(replicas, "replicas");
    int total = replicas.size();
    if (total == 0) {
      throw new IllegalArgumentException("replicas must not be empty");
    }
    for (int i = 0; i < total; i++) {
      Objects.requireNonNull(replicas.get(i), "replicas[" + i + "]");
    }

    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    ScheduledExecutorService hedgeTimer =
        Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("kata08-hedge-timer").factory());
    BlockingQueue<Attempt<T>> completions = new LinkedBlockingQueue<>();
    AttemptTracker tracker = new AttemptTracker();
    AtomicInteger launched = new AtomicInteger();
    AtomicBoolean finished = new AtomicBoolean();

    try {
      launchNext(0, replicas, executor, hedgeTimer, completions, tracker, launched, finished);

      List<Throwable> failures = new ArrayList<>(Collections.nCopies(total, null));
      int failed = 0;
      while (true) {
        Attempt<T> attempt;
        try {
          attempt = completions.take();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          finished.set(true);
          throw new IllegalStateException("Interrupted while waiting for replica results", e);
        }

        if (attempt.failure() == null) {
          finished.set(true);
          return attempt.value();
        }

        failures.set(attempt.index(), attempt.failure());
        failed++;

        int launchedNow = launched.get();
        if (failed == launchedNow) {
          if (launchedNow < total) {
            // Реплика упала до истечения hedgeDelay: не ждём таймер, пробуем следующую сразу.
            launchNext(
                launchedNow,
                replicas,
                executor,
                hedgeTimer,
                completions,
                tracker,
                launched,
                finished);
          } else {
            finished.set(true);
            throw new AllReplicasFailedException(failures);
          }
        }
      }
    } finally {
      finished.set(true);
      hedgeTimer.shutdownNow();
      tracker.cancelAll();
      executor.shutdownNow();
      awaitTermination(executor);
    }
  }

  /**
   * Пытается занять индекс следующей реплики и запустить её.
   *
   * <p>CAS по {@code launched} — единственная точка сериализации между таймером и обработчиком
   * ошибок. Проигравший просто ничего не делает: реплика уже запущена конкурентом.
   */
  private <T> void launchNext(
      int nextIndex,
      List<? extends Callable<T>> replicas,
      ExecutorService executor,
      ScheduledExecutorService hedgeTimer,
      BlockingQueue<Attempt<T>> completions,
      AttemptTracker tracker,
      AtomicInteger launched,
      AtomicBoolean finished) {
    if (nextIndex >= replicas.size() || finished.get()) {
      return;
    }
    if (!launched.compareAndSet(nextIndex, nextIndex + 1)) {
      return;
    }

    Callable<T> replica = replicas.get(nextIndex);
    boolean submitted =
        tracker.submit(
            executor,
            () -> {
              try {
                T value = replica.call();
                if (finished.compareAndSet(false, true)) {
                  completions.offer(Attempt.success(nextIndex, value));
                }
              } catch (Throwable failure) {
                // Ошибку кладём всегда: если победитель уже есть, запись просто останется
                // невостребованной, зато главный поток не потеряет случай «все упали».
                completions.offer(Attempt.failure(nextIndex, failure));
              }
            });
    if (!submitted) {
      // Executor уже закрыт/отменён. Если главный поток ещё ждёт, он не должен зависнуть:
      // сообщаем об этой попытке как об ошибке.
      completions.offer(
          Attempt.failure(
              nextIndex, new IllegalStateException("replica " + nextIndex + " was not started")));
      return;
    }

    if (nextIndex + 1 < replicas.size()) {
      hedgeTimer.schedule(
          () ->
              launchNext(
                  nextIndex + 1,
                  replicas,
                  executor,
                  hedgeTimer,
                  completions,
                  tracker,
                  launched,
                  finished),
          hedgeDelayNanos,
          TimeUnit.NANOSECONDS);
    }
  }

  private static void awaitTermination(ExecutorService executor) {
    try {
      executor.awaitTermination(CANCEL_AWAIT_MILLIS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** {@link Duration#toNanos()} с насыщением: гигантская задержка не должна ронять конструктор. */
  private static long toNanosSaturated(Duration duration) {
    try {
      return duration.toNanos();
    } catch (ArithmeticException overflow) {
      return Long.MAX_VALUE;
    }
  }

  /** Результат одной попытки; ровно одно из полей {@code value}/{@code failure} заполнено. */
  private record Attempt<T>(int index, T value, Throwable failure) {

    static <T> Attempt<T> success(int index, T value) {
      return new Attempt<>(index, value, null);
    }

    static <T> Attempt<T> failure(int index, Throwable failure) {
      return new Attempt<>(index, null, Objects.requireNonNull(failure, "failure"));
    }
  }

  /**
   * Регистрирует futures и умеет атомарно отменить их все.
   *
   * <p>Обычный {@code CopyOnWriteArrayList} здесь недостаточен: между проверкой «победитель уже
   * есть» в таймере и добавлением future в список остаётся окно, в котором {@code cancelAll}
   * пройдёт по списку и не увидит новую задачу. Блокировка закрывает это окно: либо задача
   * добавляется раньше отмены и будет отменена, либо видит {@code cancelled} и не запускается.
   */
  private static final class AttemptTracker {
    private final List<Future<?>> futures = new ArrayList<>();
    private boolean cancelled;

    synchronized boolean submit(ExecutorService executor, Runnable task) {
      if (cancelled) {
        return false;
      }
      try {
        futures.add(executor.submit(task));
        return true;
      } catch (RejectedExecutionException e) {
        return false;
      }
    }

    synchronized void cancelAll() {
      cancelled = true;
      for (Future<?> future : futures) {
        future.cancel(true);
      }
    }
  }
}
