package dev.kata.patterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import dev.kata.patterns.Kata11_CircuitBreaker.CircuitBreakerOpenException;
import dev.kata.patterns.Kata11_CircuitBreaker.State;
import dev.kata.solutions.patterns.Kata11_CircuitBreakerSolution;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 11.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata11* -DuseSolution=true</pre>
 *
 * <p>Все переходы проверяются на управляемых часах: тесты не спят и не зависят от загрузки машины.
 */
@DisplayName("Kata 11: Circuit Breaker со скользящим окном")
class Kata11_CircuitBreakerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata11_CircuitBreaker newBreaker(
      int window, int minCalls, double threshold, Duration openDuration, int permits) {
    return USE_SOLUTION
        ? new Kata11_CircuitBreakerSolution(window, minCalls, threshold, openDuration, permits)
        : new Kata11_CircuitBreaker(window, minCalls, threshold, openDuration, permits);
  }

  private Kata11_CircuitBreaker newBreaker(
      int window,
      int minCalls,
      double threshold,
      Duration openDuration,
      int permits,
      LongSupplier clock) {
    return USE_SOLUTION
        ? new Kata11_CircuitBreakerSolution(
            window, minCalls, threshold, openDuration, permits, clock)
        : new Kata11_CircuitBreaker(window, minCalls, threshold, openDuration, permits, clock);
  }

  @Test
  @DisplayName("Порог доли ошибок открывает предохранитель после minimumCalls")
  void opensWhenFailureRateExceedsThreshold() throws Exception {
    MutableClock clock = new MutableClock();
    Kata11_CircuitBreaker breaker = newBreaker(10, 5, 0.5, Duration.ofSeconds(10), 2, clock);

    for (int i = 0; i < 4; i++) {
      succeed(breaker);
    }
    fail(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.CLOSED); // 1 ошибка из 5

    for (int i = 0; i < 3; i++) {
      fail(breaker);
    }
    assertThat(breaker.currentState()).isEqualTo(State.CLOSED); // 4 из 8 = 0.5, порог строгий

    fail(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.OPEN); // 5 из 9 > 0.5
  }

  @Test
  @DisplayName("minimumCalls защищает от открытия после первых же отказов")
  void minimumCallsPreventsEarlyTrip() {
    MutableClock clock = new MutableClock();
    Kata11_CircuitBreaker breaker = newBreaker(10, 5, 0.5, Duration.ofSeconds(10), 2, clock);

    for (int i = 0; i < 4; i++) {
      fail(breaker);
      assertThat(breaker.currentState()).isEqualTo(State.CLOSED);
    }

    fail(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.OPEN);
  }

  @Test
  @DisplayName("OPEN отклоняет вызовы без выполнения функции, пока не истёк openDuration")
  void openRejectsWithoutInvokingTarget() throws Exception {
    MutableClock clock = new MutableClock();
    Kata11_CircuitBreaker breaker = newBreaker(10, 2, 0.5, Duration.ofSeconds(10), 2, clock);
    fail(breaker);
    fail(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.OPEN);

    AtomicInteger invoked = new AtomicInteger();
    clock.advance(Duration.ofSeconds(9));
    assertThat(breaker.currentState()).isEqualTo(State.OPEN);

    assertThatThrownBy(
            () ->
                breaker.execute(
                    () -> {
                      invoked.incrementAndGet();
                      return "should-not-run";
                    }))
        .isInstanceOf(CircuitBreakerOpenException.class)
        .satisfies(
            thrown ->
                assertThat(((CircuitBreakerOpenException) thrown).state()).isEqualTo(State.OPEN));

    assertThat(invoked).hasValue(0);
    assertThat(breaker.rejectedCalls()).isEqualTo(1);
  }

  @Test
  @DisplayName("HALF_OPEN: все разрешённые пробы успешны — возврат в CLOSED")
  void halfOpenClosesAfterAllProbesSucceed() throws Exception {
    MutableClock clock = new MutableClock();
    Kata11_CircuitBreaker breaker = newBreaker(10, 2, 0.5, Duration.ofSeconds(10), 2, clock);
    fail(breaker);
    fail(breaker);

    clock.advance(Duration.ofSeconds(10));
    assertThat(breaker.currentState()).isEqualTo(State.HALF_OPEN);

    succeed(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.HALF_OPEN);
    succeed(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.CLOSED);

    succeed(breaker); // после закрытия обычные вызовы снова проходят
    assertThat(breaker.currentState()).isEqualTo(State.CLOSED);
  }

  @Test
  @DisplayName("HALF_OPEN: неудачная проба возвращает OPEN и перезапускает openDuration")
  void halfOpenFailureReopensAndResetsTimer() {
    MutableClock clock = new MutableClock();
    Kata11_CircuitBreaker breaker = newBreaker(10, 2, 0.5, Duration.ofSeconds(10), 2, clock);
    fail(breaker);
    fail(breaker);

    clock.advance(Duration.ofSeconds(10));
    assertThat(breaker.currentState()).isEqualTo(State.HALF_OPEN);

    fail(breaker); // первая проба упала
    assertThat(breaker.currentState()).isEqualTo(State.OPEN);

    clock.advance(Duration.ofSeconds(9));
    assertThat(breaker.currentState()).isEqualTo(State.OPEN);
    clock.advance(Duration.ofSeconds(1));
    assertThat(breaker.currentState()).isEqualTo(State.HALF_OPEN);
  }

  @Test
  @DisplayName("HALF_OPEN: из 100 конкурентных потоков проходят ровно permittedCallsInHalfOpen")
  void halfOpenPermitsAreEnforcedConcurrently() throws Exception {
    MutableClock clock = new MutableClock();
    int permits = 5;
    int threads = 100;
    Kata11_CircuitBreaker breaker = newBreaker(10, 2, 0.5, Duration.ofSeconds(1), permits, clock);
    fail(breaker);
    fail(breaker);
    clock.advance(Duration.ofSeconds(1));
    assertThat(breaker.currentState()).isEqualTo(State.HALF_OPEN);

    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch probesEntered = new CountDownLatch(permits);
    CountDownLatch releaseProbes = new CountDownLatch(1);
    AtomicInteger passed = new AtomicInteger();
    AtomicInteger rejected = new AtomicInteger();
    List<Throwable> unexpectedErrors = java.util.Collections.synchronizedList(new ArrayList<>());

    ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    try {
      List<Future<?>> futures = new ArrayList<>(threads);
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  try {
                    breaker.execute(
                        () -> {
                          probesEntered.countDown();
                          releaseProbes.await(10, TimeUnit.SECONDS);
                          return "probe";
                        });
                    passed.incrementAndGet();
                  } catch (CircuitBreakerOpenException e) {
                    rejected.incrementAndGet();
                  } catch (Throwable t) {
                    unexpectedErrors.add(t);
                  }
                  return null;
                }));
      }

      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(probesEntered.await(10, TimeUnit.SECONDS)).isTrue();
      await().atMost(Duration.ofSeconds(10)).until(() -> rejected.get() == threads - permits);

      releaseProbes.countDown();
      for (Future<?> future : futures) {
        future.get(10, TimeUnit.SECONDS);
      }
    } finally {
      releaseProbes.countDown();
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(unexpectedErrors).isEmpty();
    assertThat(passed).hasValue(permits);
    assertThat(rejected).hasValue(threads - permits);
    assertThat(breaker.rejectedCalls()).isEqualTo(threads - permits);
    assertThat(breaker.currentState()).isEqualTo(State.CLOSED);
  }

  @Test
  @DisplayName("Скользящее окно вытесняет старые результаты")
  void slidingWindowEvictsOldResults() throws Exception {
    MutableClock clock = new MutableClock();
    Kata11_CircuitBreaker breaker = newBreaker(4, 4, 0.75, Duration.ofSeconds(10), 1, clock);

    fail(breaker);
    fail(breaker);
    fail(breaker);
    succeed(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.CLOSED); // 3/4 = 0.75, не больше порога

    succeed(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.CLOSED); // старая ошибка вытеснена, 2/4

    fail(breaker);
    fail(breaker);
    fail(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.CLOSED); // 3/4

    fail(breaker);
    assertThat(breaker.currentState()).isEqualTo(State.OPEN); // 4/4 > 0.75
  }

  @Test
  @DisplayName("Исключение целевой функции пробрасывается без изменений и учитывается")
  void targetExceptionIsPropagatedAndCounted() {
    MutableClock clock = new MutableClock();
    Kata11_CircuitBreaker breaker = newBreaker(10, 1, 0.5, Duration.ofSeconds(10), 1, clock);

    RuntimeException runtimeFailure = new RuntimeException("boom");
    assertThatThrownBy(
            () ->
                breaker.execute(
                    () -> {
                      throw runtimeFailure;
                    }))
        .isSameAs(runtimeFailure);
    assertThat(breaker.currentState()).isEqualTo(State.OPEN);
  }

  @Test
  @DisplayName("Проверяемое исключение также пробрасывается и учитывается")
  void checkedExceptionIsPropagated() {
    MutableClock clock = new MutableClock();
    Kata11_CircuitBreaker breaker = newBreaker(10, 10, 0.5, Duration.ofSeconds(10), 1, clock);
    Exception checked = new Exception("checked");

    assertThatThrownBy(
            () ->
                breaker.execute(
                    () -> {
                      throw checked;
                    }))
        .isSameAs(checked);
    assertThat(breaker.currentState()).isEqualTo(State.CLOSED); // minimumCalls ещё не набран
  }

  @Test
  @DisplayName("Валидация конфигурации")
  void invalidConfiguration() {
    MutableClock clock = new MutableClock();
    Duration tenSeconds = Duration.ofSeconds(10);

    assertThatThrownBy(() -> newBreaker(0, 1, 0.5, tenSeconds, 1, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBreaker(10, 0, 0.5, tenSeconds, 1, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBreaker(10, 11, 0.5, tenSeconds, 1, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBreaker(10, 5, 0.0, tenSeconds, 1, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBreaker(10, 5, 1.5, tenSeconds, 1, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBreaker(10, 5, Double.NaN, tenSeconds, 1, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBreaker(10, 5, 0.5, Duration.ZERO, 1, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBreaker(10, 5, 0.5, Duration.ofMillis(-1), 1, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBreaker(10, 5, 0.5, tenSeconds, 0, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBreaker(10, 5, 0.5, null, 1, clock))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newBreaker(10, 5, 0.5, tenSeconds, 1, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("null-функция запрещена")
  void nullCallRejected() {
    MutableClock clock = new MutableClock();
    Kata11_CircuitBreaker breaker = newBreaker(10, 5, 0.5, Duration.ofSeconds(10), 1, clock);

    assertThatThrownBy(() -> breaker.execute(null)).isInstanceOf(NullPointerException.class);
  }

  private static void succeed(Kata11_CircuitBreaker breaker) throws Exception {
    breaker.execute(() -> "ok");
  }

  private static void fail(Kata11_CircuitBreaker breaker) {
    try {
      breaker.execute(
          () -> {
            throw new IllegalStateException("simulated downstream failure");
          });
      throw new AssertionError("expected failure");
    } catch (IllegalStateException expected) {
      // ожидаемо: ошибка целевой функции
    } catch (Exception unexpected) {
      throw new AssertionError("unexpected exception", unexpected);
    }
  }

  /** Управляемые монотонные часы: тесты двигают время вручную. */
  private static final class MutableClock implements LongSupplier {
    private final AtomicLong nanos = new AtomicLong(1_000_000_000L);

    @Override
    public long getAsLong() {
      return nanos.get();
    }

    void advance(Duration duration) {
      nanos.addAndGet(duration.toNanos());
    }
  }
}
