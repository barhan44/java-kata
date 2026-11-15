package dev.kata.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.concurrency.Kata08_StructuredScatterGather.AllReplicasFailedException;
import dev.kata.solutions.concurrency.Kata08_StructuredScatterGatherSolution;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 08.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata08* -DuseSolution=true</pre>
 */
@DisplayName("Kata 08: hedged requests на виртуальных потоках")
class Kata08_StructuredScatterGatherTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata08_StructuredScatterGather newScatterGather(Duration hedgeDelay) {
    return USE_SOLUTION
        ? new Kata08_StructuredScatterGatherSolution(hedgeDelay)
        : new Kata08_StructuredScatterGather(hedgeDelay);
  }

  @Test
  @DisplayName("Быстрый ответ первой реплики: вторая не вызывается")
  void fastFirstReplicaDoesNotTriggerHedge() {
    Kata08_StructuredScatterGather scatterGather = newScatterGather(Duration.ofMillis(500));
    AtomicInteger firstCalls = new AtomicInteger();
    AtomicInteger secondCalls = new AtomicInteger();

    String result =
        scatterGather.execute(
            List.of(
                () -> {
                  firstCalls.incrementAndGet();
                  return "first";
                },
                () -> {
                  secondCalls.incrementAndGet();
                  return "second";
                }));

    assertThat(result).isEqualTo("first");
    assertThat(firstCalls).hasValue(1);
    assertThat(secondCalls).hasValue(0);
  }

  @Test
  @DisplayName("Реплики выполняются на виртуальных потоках")
  void replicasRunOnVirtualThreads() {
    Kata08_StructuredScatterGather scatterGather = newScatterGather(Duration.ofSeconds(1));
    AtomicBoolean virtual = new AtomicBoolean();

    String result =
        scatterGather.execute(
            List.of(
                () -> {
                  virtual.set(Thread.currentThread().isVirtual());
                  return "ok";
                }));

    assertThat(result).isEqualTo("ok");
    assertThat(virtual).isTrue();
  }

  @Test
  @DisplayName("Медленная первая реплика: хедж отвечает, первая получает InterruptedException")
  void slowFirstIsHedgedAndInterrupted() throws Exception {
    Kata08_StructuredScatterGather scatterGather = newScatterGather(Duration.ofMillis(50));
    CountDownLatch firstInterrupted = new CountDownLatch(1);
    AtomicInteger secondCalls = new AtomicInteger();

    String result =
        scatterGather.execute(
            List.of(
                () -> {
                  try {
                    Thread.sleep(10_000);
                    return "first";
                  } catch (InterruptedException e) {
                    firstInterrupted.countDown();
                    throw e;
                  }
                },
                () -> {
                  secondCalls.incrementAndGet();
                  Thread.sleep(30);
                  return "second";
                }));

    assertThat(result).isEqualTo("second");
    assertThat(secondCalls).hasValue(1);
    assertThat(firstInterrupted.await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @DisplayName("Падение первой реплики: вторая запускается сразу, без ожидания hedgeDelay")
  void firstFailureFallsBackImmediately() {
    Kata08_StructuredScatterGather scatterGather = newScatterGather(Duration.ofSeconds(30));
    AtomicInteger calls = new AtomicInteger();

    String result =
        scatterGather.execute(
            List.of(
                () -> {
                  calls.incrementAndGet();
                  throw new IllegalStateException("replica-1 down");
                },
                () -> {
                  calls.incrementAndGet();
                  return "ok";
                }));

    assertThat(result).isEqualTo("ok");
    assertThat(calls).hasValue(2);
  }

  @Test
  @DisplayName("Последовательное хеджирование трёх реплик: побеждает третья, первые две прерваны")
  void multipleHedgesAreLaunchedSequentially() throws Exception {
    Kata08_StructuredScatterGather scatterGather = newScatterGather(Duration.ofMillis(50));
    CountDownLatch interrupted = new CountDownLatch(2);
    AtomicInteger calls = new AtomicInteger();

    String result =
        scatterGather.execute(
            List.of(
                () -> {
                  calls.incrementAndGet();
                  return sleepUnlessInterrupted(10_000, interrupted);
                },
                () -> {
                  calls.incrementAndGet();
                  return sleepUnlessInterrupted(10_000, interrupted);
                },
                () -> {
                  calls.incrementAndGet();
                  Thread.sleep(20);
                  return "third";
                }));

    assertThat(result).isEqualTo("third");
    assertThat(calls).hasValue(3);
    assertThat(interrupted.await(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @DisplayName("Все реплики упали: агрегированное исключение в порядке реплик")
  void allReplicasFailAggregated() {
    Kata08_StructuredScatterGather scatterGather = newScatterGather(Duration.ofMillis(20));

    assertThatThrownBy(
            () ->
                scatterGather.execute(
                    List.of(
                        () -> {
                          throw new IllegalStateException("one");
                        },
                        () -> {
                          throw new IllegalArgumentException("two");
                        })))
        .isInstanceOf(AllReplicasFailedException.class)
        .satisfies(
            thrown -> {
              AllReplicasFailedException aggregate = (AllReplicasFailedException) thrown;
              assertThat(aggregate.failures())
                  .hasSize(2)
                  .satisfiesExactly(
                      first -> assertThat(first).isInstanceOf(IllegalStateException.class),
                      second -> assertThat(second).isInstanceOf(IllegalArgumentException.class));
              assertThat(aggregate.getSuppressed()).hasSize(2);
            });
  }

  @Test
  @DisplayName("Таймауты всех реплик агрегируются как ошибки")
  void allReplicaTimeoutsAreAggregated() {
    Kata08_StructuredScatterGather scatterGather = newScatterGather(Duration.ofMillis(20));

    assertThatThrownBy(
            () ->
                scatterGather.execute(
                    List.of(
                        () -> {
                          throw new TimeoutException("r1 timeout");
                        },
                        () -> {
                          throw new TimeoutException("r2 timeout");
                        })))
        .isInstanceOf(AllReplicasFailedException.class)
        .satisfies(
            thrown ->
                assertThat(((AllReplicasFailedException) thrown).failures())
                    .hasSize(2)
                    .allSatisfy(
                        failure -> assertThat(failure).isInstanceOf(TimeoutException.class)));
  }

  @Test
  @DisplayName("Одна реплика: ошибка сразу оборачивается в агрегированное исключение")
  void singleReplicaFailureIsAggregated() {
    Kata08_StructuredScatterGather scatterGather = newScatterGather(Duration.ofMillis(20));

    assertThatThrownBy(
            () ->
                scatterGather.execute(
                    List.of(
                        () -> {
                          throw new IllegalStateException("only");
                        })))
        .isInstanceOf(AllReplicasFailedException.class)
        .satisfies(
            thrown ->
                assertThat(((AllReplicasFailedException) thrown).failures())
                    .singleElement()
                    .isInstanceOf(IllegalStateException.class));
  }

  @Test
  @DisplayName("Валидация: пустой список, null-элементы и некорректный hedgeDelay")
  void invalidArguments() {
    Kata08_StructuredScatterGather scatterGather = newScatterGather(Duration.ofMillis(20));

    assertThatThrownBy(() -> scatterGather.execute(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> scatterGather.execute(null)).isInstanceOf(NullPointerException.class);

    Callable<String> nullReplica = null;
    assertThatThrownBy(() -> scatterGather.execute(java.util.Arrays.asList(nullReplica)))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> newScatterGather(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newScatterGather(Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newScatterGather(Duration.ofMillis(-1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static String sleepUnlessInterrupted(long millis, CountDownLatch interrupted)
      throws InterruptedException {
    try {
      Thread.sleep(millis);
      return "unexpected";
    } catch (InterruptedException e) {
      interrupted.countDown();
      throw e;
    }
  }
}
