package dev.kata.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import dev.kata.solutions.concurrency.Kata09_KeyedSequentialExecutorSolution;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 09.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata09* -DuseSolution=true</pre>
 */
@DisplayName("Kata 09: последовательное выполнение задач по ключу")
class Kata09_KeyedSequentialExecutorTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private <K> Kata09_KeyedSequentialExecutor<K> newExecutor() {
    return USE_SOLUTION
        ? new Kata09_KeyedSequentialExecutorSolution<>()
        : new Kata09_KeyedSequentialExecutor<>();
  }

  @Test
  @DisplayName("Один ключ: задачи выполняются строго в порядке поступления")
  void sameKeyRunsInFifoOrder() throws Exception {
    try (Kata09_KeyedSequentialExecutor<String> executor = newExecutor()) {
      int taskCount = 1_000;
      List<Integer> executionOrder = Collections.synchronizedList(new ArrayList<>());
      List<CompletableFuture<Integer>> futures = new ArrayList<>(taskCount);

      for (int i = 0; i < taskCount; i++) {
        int value = i;
        futures.add(
            executor.submit(
                "account-42",
                () -> {
                  executionOrder.add(value);
                  return value;
                }));
      }

      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);

      assertThat(executionOrder)
          .containsExactlyElementsOf(IntStream.range(0, taskCount).boxed().toList());
      for (int i = 0; i < taskCount; i++) {
        assertThat(futures.get(i).get(1, TimeUnit.SECONDS)).isEqualTo(i);
      }
      await().atMost(Duration.ofSeconds(5)).until(() -> executor.activeKeyCount() == 0);
    }
  }

  @Test
  @DisplayName("Разные ключи выполняются параллельно")
  void differentKeysRunInParallel() throws Exception {
    try (Kata09_KeyedSequentialExecutor<Integer> executor = newExecutor()) {
      int keys = 4;
      CountDownLatch started = new CountDownLatch(keys);
      CountDownLatch release = new CountDownLatch(1);
      List<CompletableFuture<Integer>> futures = new ArrayList<>();

      for (int key = 0; key < keys; key++) {
        int value = key;
        futures.add(
            executor.submit(
                key,
                () -> {
                  started.countDown();
                  if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("parallelism latch timed out");
                  }
                  return value;
                }));
      }

      try {
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
      } finally {
        release.countDown();
      }

      for (int key = 0; key < keys; key++) {
        assertThat(futures.get(key).get(5, TimeUnit.SECONDS)).isEqualTo(key);
      }
    }
  }

  @Test
  @DisplayName("Конкурентный submit одного ключа: задачи не перекрываются")
  void concurrentSubmitsOfSameKeyNeverOverlap() throws Exception {
    try (Kata09_KeyedSequentialExecutor<Integer> executor = newExecutor()) {
      int submitterThreads = 8;
      int tasksPerThread = 100;
      int keyCount = 4;
      List<CompletableFuture<Integer>> futures =
          Collections.synchronizedList(new ArrayList<>(submitterThreads * tasksPerThread));
      ConcurrentHashMap<Integer, AtomicInteger> activePerKey = new ConcurrentHashMap<>();
      AtomicBoolean overlap = new AtomicBoolean();

      ExecutorService submitters = Executors.newFixedThreadPool(submitterThreads);
      CountDownLatch ready = new CountDownLatch(submitterThreads);
      CountDownLatch start = new CountDownLatch(1);
      try {
        List<Future<?>> submitterFutures = new ArrayList<>();
        for (int thread = 0; thread < submitterThreads; thread++) {
          int threadId = thread;
          submitterFutures.add(
              submitters.submit(
                  () -> {
                    ready.countDown();
                    start.await();
                    for (int i = 0; i < tasksPerThread; i++) {
                      int key = (threadId + i) % keyCount;
                      futures.add(
                          executor.submit(
                              key,
                              () -> {
                                AtomicInteger active =
                                    activePerKey.computeIfAbsent(key, k -> new AtomicInteger());
                                if (active.incrementAndGet() != 1) {
                                  overlap.set(true);
                                }
                                try {
                                  LockSupport.parkNanos(100_000);
                                  return key;
                                } finally {
                                  active.decrementAndGet();
                                }
                              }));
                    }
                    return null;
                  }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> submitter : submitterFutures) {
          submitter.get(10, TimeUnit.SECONDS);
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
            .get(30, TimeUnit.SECONDS);
      } finally {
        submitters.shutdownNow();
        assertThat(submitters.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
      }

      assertThat(overlap).isFalse();
      assertThat(futures).hasSize(submitterThreads * tasksPerThread);
      for (CompletableFuture<Integer> future : futures) {
        assertThat(future.get(1, TimeUnit.SECONDS)).isNotNull();
      }
      assertThat(activePerKey.values()).allSatisfy(count -> assertThat(count).hasValue(0));
    }
  }

  @Test
  @DisplayName("Утечка памяти: после выполнения уникальных ключей карта очищается")
  void idleKeysAreRemovedFromInternalMap() throws Exception {
    try (Kata09_KeyedSequentialExecutor<String> executor = newExecutor()) {
      int uniqueKeys = 2_000;
      List<CompletableFuture<Integer>> futures = new ArrayList<>(uniqueKeys);

      for (int i = 0; i < uniqueKeys; i++) {
        futures.add(executor.submit("request-" + i, () -> 1));
      }
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(20, TimeUnit.SECONDS);

      await().atMost(Duration.ofSeconds(5)).until(() -> executor.activeKeyCount() == 0);
    }
  }

  @Test
  @DisplayName("Ключ присутствует в карте, пока его задача не завершилась")
  void activeKeyCountTracksInFlightWork() throws Exception {
    try (Kata09_KeyedSequentialExecutor<String> executor = newExecutor()) {
      CountDownLatch started = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);

      CompletableFuture<Integer> future =
          executor.submit(
              "slow",
              () -> {
                started.countDown();
                release.await(5, TimeUnit.SECONDS);
                return 7;
              });

      assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
      await().atMost(Duration.ofSeconds(2)).until(() -> executor.activeKeyCount() == 1);

      release.countDown();
      assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo(7);
      await().atMost(Duration.ofSeconds(5)).until(() -> executor.activeKeyCount() == 0);
    }
  }

  @Test
  @DisplayName("Исключение одной задачи не ломает очередь следующих задач ключа")
  void failureDoesNotBreakTheKeyChain() throws Exception {
    try (Kata09_KeyedSequentialExecutor<String> executor = newExecutor()) {
      CompletableFuture<Integer> failing =
          executor.submit(
              "key",
              () -> {
                throw new IllegalStateException("boom");
              });
      CompletableFuture<Integer> afterRuntimeFailure = executor.submit("key", () -> 42);
      CompletableFuture<Integer> afterError =
          executor.submit(
              "key",
              () -> {
                throw new AssertionError("simulated fatal");
              });
      CompletableFuture<Integer> afterAll = executor.submit("key", () -> 99);

      assertThatThrownBy(failing::join)
          .isInstanceOf(CompletionException.class)
          .hasCauseInstanceOf(IllegalStateException.class);
      assertThat(afterRuntimeFailure.get(5, TimeUnit.SECONDS)).isEqualTo(42);
      assertThatThrownBy(afterError::join)
          .isInstanceOf(CompletionException.class)
          .hasCauseInstanceOf(AssertionError.class);
      assertThat(afterAll.get(5, TimeUnit.SECONDS)).isEqualTo(99);
    }
  }

  @Test
  @DisplayName("Задача может вернуть null")
  void nullResultIsAllowed() throws Exception {
    try (Kata09_KeyedSequentialExecutor<String> executor = newExecutor()) {
      assertThat(executor.submit("key", () -> null).get(5, TimeUnit.SECONDS)).isNull();
    }
  }

  @Test
  @DisplayName("submit после close возвращает future с RejectedExecutionException")
  void submitAfterCloseReturnsRejectedFuture() {
    Kata09_KeyedSequentialExecutor<String> executor = newExecutor();
    executor.close();
    executor.close(); // идемпотентность

    CompletableFuture<Integer> rejected = executor.submit("key", () -> 1);

    assertThatThrownBy(rejected::join)
        .isInstanceOf(CompletionException.class)
        .hasCauseInstanceOf(RejectedExecutionException.class);
  }

  @Test
  @DisplayName("null-ключ и null-задача запрещены")
  void nullsRejected() {
    try (Kata09_KeyedSequentialExecutor<String> executor = newExecutor()) {
      assertThatThrownBy(() -> executor.submit(null, () -> 1))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> executor.submit("key", null))
          .isInstanceOf(NullPointerException.class);
    }
  }
}
