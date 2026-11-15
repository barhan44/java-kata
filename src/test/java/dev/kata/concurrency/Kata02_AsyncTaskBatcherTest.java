package dev.kata.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.concurrency.Kata02_AsyncTaskBatcherSolution;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 02.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata02* -DuseSolution=true</pre>
 */
@DisplayName("Kata 02: асинхронный батчер запросов")
class Kata02_AsyncTaskBatcherTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private <T, R> Kata02_AsyncTaskBatcher<T, R> newBatcher(
      int maxBatchSize,
      Duration maxWaitTime,
      java.util.function.Function<List<T>, List<R>> batchFunction,
      ExecutorService executor) {
    return USE_SOLUTION
        ? new Kata02_AsyncTaskBatcherSolution<>(maxBatchSize, maxWaitTime, batchFunction, executor)
        : new Kata02_AsyncTaskBatcher<>(maxBatchSize, maxWaitTime, batchFunction, executor);
  }

  @Test
  @DisplayName("Пачка отправляется при достижении maxBatchSize, результаты в правильном порядке")
  void flushesWhenBatchSizeReached() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    List<List<Integer>> batches = new CopyOnWriteArrayList<>();
    Kata02_AsyncTaskBatcher<Integer, Integer> batcher =
        newBatcher(
            10,
            Duration.ofSeconds(10),
            items -> {
              batches.add(List.copyOf(items));
              return items.stream().map(i -> i * 2).toList();
            },
            pool);
    try {
      List<CompletableFuture<Integer>> futures = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        futures.add(batcher.submit(i));
      }

      for (int i = 0; i < 10; i++) {
        assertThat(futures.get(i).get(2, TimeUnit.SECONDS)).isEqualTo(i * 2);
      }
      assertThat(batches).hasSize(1);
      assertThat(batches.get(0)).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    } finally {
      batcher.close();
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("Одиночный запрос уходит по таймауту, а не висит вечно")
  void flushesByTimeout() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    AtomicInteger batches = new AtomicInteger();
    Kata02_AsyncTaskBatcher<Integer, Integer> batcher =
        newBatcher(
            100,
            Duration.ofMillis(80),
            items -> {
              batches.incrementAndGet();
              return new ArrayList<>(items);
            },
            pool);
    try {
      long start = System.nanoTime();
      CompletableFuture<Integer> future = batcher.submit(42);
      assertThat(future.get(2, TimeUnit.SECONDS)).isEqualTo(42);
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      assertThat(elapsedMillis).isGreaterThanOrEqualTo(50);
      assertThat(batches).hasValue(1);
    } finally {
      batcher.close();
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("Конкурентные submit: ни один элемент не потерян, размер пачки не превышен")
  void concurrentSubmissionsAreNotLost() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(4);
    int threads = 8;
    int perThread = 100;
    List<Integer> batchSizes = Collections.synchronizedList(new ArrayList<>());
    Map<Integer, Integer> seen = new ConcurrentHashMap<>();

    Kata02_AsyncTaskBatcher<Integer, Integer> batcher =
        newBatcher(
            16,
            Duration.ofMillis(25),
            items -> {
              batchSizes.add(items.size());
              items.forEach(i -> seen.put(i, i * 10));
              return items.stream().map(i -> i * 10).toList();
            },
            pool);
    try {
      List<CompletableFuture<Integer>> futures = Collections.synchronizedList(new ArrayList<>());
      ExecutorService submitters = Executors.newFixedThreadPool(threads);
      try {
        List<Future<?>> tasks = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
          int base = t * perThread;
          tasks.add(
              submitters.submit(
                  () -> {
                    for (int i = 0; i < perThread; i++) {
                      futures.add(batcher.submit(base + i));
                    }
                  }));
        }
        for (Future<?> task : tasks) {
          task.get(5, TimeUnit.SECONDS);
        }
      } finally {
        submitters.shutdownNow();
      }

      for (CompletableFuture<Integer> future : futures) {
        assertThat(future.get(10, TimeUnit.SECONDS)).isNotNull();
      }
      assertThat(futures).hasSize(threads * perThread);
      assertThat(seen).hasSize(threads * perThread);
      assertThat(batchSizes).isNotEmpty().allMatch(size -> size >= 1 && size <= 16);
    } finally {
      batcher.close();
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("Исключение batchFunction завершает все future пачки")
  void batchFailurePropagates() {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    Kata02_AsyncTaskBatcher<Integer, Integer> batcher =
        newBatcher(
            2,
            Duration.ofSeconds(5),
            items -> {
              throw new IllegalStateException("downstream is down");
            },
            pool);
    try {
      CompletableFuture<Integer> first = batcher.submit(1);
      CompletableFuture<Integer> second = batcher.submit(2);

      assertThatThrownBy(first::join)
          .hasCauseInstanceOf(IllegalStateException.class)
          .hasMessageContaining("downstream is down");
      assertThatThrownBy(second::join)
          .hasCauseInstanceOf(IllegalStateException.class)
          .hasMessageContaining("downstream is down");
    } finally {
      batcher.close();
      pool.shutdownNow();
    }
  }

  @Test
  @DisplayName("Неверный размер результата — понятная ошибка, а не тихая порча данных")
  void wrongResultSizeFailsFutures() {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    Kata02_AsyncTaskBatcher<Integer, Integer> batcher =
        newBatcher(2, Duration.ofSeconds(5), items -> List.of(), pool);
    try {
      CompletableFuture<Integer> first = batcher.submit(1);
      batcher.submit(2);

      assertThatThrownBy(first::join)
          .hasCauseInstanceOf(IllegalStateException.class)
          .hasMessageContaining("must return exactly 2 results");
    } finally {
      batcher.close();
      pool.shutdownNow();
    }
  }

  @Test
  @DisplayName("close: финальная пачка отправляется, новые submit отклоняются")
  void closeFlushesAndRejectsNewItems() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    Kata02_AsyncTaskBatcher<Integer, Integer> batcher =
        newBatcher(100, Duration.ofSeconds(10), items -> new ArrayList<>(items), pool);

    CompletableFuture<Integer> pending = batcher.submit(7);
    batcher.close();
    batcher.close(); // идемпотентно

    assertThat(pending.get(2, TimeUnit.SECONDS)).isEqualTo(7);

    CompletableFuture<Integer> rejected = batcher.submit(8);
    assertThat(rejected.isCompletedExceptionally()).isTrue();
    assertThatThrownBy(rejected::join)
        .hasCauseInstanceOf(IllegalStateException.class)
        .hasMessageContaining("closed");

    pool.shutdownNow();
  }

  @Test
  @DisplayName("Валидация аргументов конструктора")
  void constructorValidation() {
    ExecutorService pool = Executors.newFixedThreadPool(1);
    try {
      assertThatThrownBy(() -> newBatcher(0, Duration.ofSeconds(1), items -> items, pool))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> newBatcher(1, Duration.ZERO, items -> items, pool))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> newBatcher(1, Duration.ofSeconds(-1), items -> items, pool))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> newBatcher(1, Duration.ofSeconds(1), null, pool))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> newBatcher(1, Duration.ofSeconds(1), items -> items, null))
          .isInstanceOf(NullPointerException.class);
    } finally {
      pool.shutdownNow();
    }
  }
}
