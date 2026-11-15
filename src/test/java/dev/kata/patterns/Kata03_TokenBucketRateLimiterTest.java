package dev.kata.patterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import dev.kata.solutions.patterns.Kata03_TokenBucketRateLimiterSolution;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 03.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata03* -DuseSolution=true</pre>
 */
@DisplayName("Kata 03: Token Bucket rate limiter без блокировок")
class Kata03_TokenBucketRateLimiterTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata03_TokenBucketRateLimiter newLimiter(long capacity, double refillPerSecond) {
    return USE_SOLUTION
        ? new Kata03_TokenBucketRateLimiterSolution(capacity, refillPerSecond)
        : new Kata03_TokenBucketRateLimiter(capacity, refillPerSecond);
  }

  @Test
  @DisplayName("Свежее ведро: ровно capacity разрешений, затем отказ")
  void initialBurstIsLimitedByCapacity() {
    Kata03_TokenBucketRateLimiter limiter = newLimiter(3, 1.0);

    assertThat(limiter.tryAcquire()).isTrue();
    assertThat(limiter.tryAcquire()).isTrue();
    assertThat(limiter.tryAcquire()).isTrue();
    assertThat(limiter.tryAcquire()).isFalse();
    assertThat(limiter.availableTokens()).isBetween(0.0, 1.0);
  }

  @Test
  @DisplayName("Пополнение по nanoTime: после ожидания токен появляется")
  void tokensAreRefilledOverTime() {
    Kata03_TokenBucketRateLimiter limiter = newLimiter(5, 200.0); // 1 токен / 5 мс

    for (int i = 0; i < 5; i++) {
      assertThat(limiter.tryAcquire()).isTrue();
    }
    assertThat(limiter.tryAcquire()).isFalse();

    await()
        .atMost(Duration.ofSeconds(2))
        .pollInterval(Duration.ofMillis(5))
        .until(() -> limiter.availableTokens() >= 1.0);

    assertThat(limiter.tryAcquire()).isTrue();
  }

  @Test
  @DisplayName("Мульти-токены: tryAcquire(permits) атомарен и уважает capacity")
  void multiPermitAcquire() {
    Kata03_TokenBucketRateLimiter limiter = newLimiter(100, 1000.0);

    assertThat(limiter.tryAcquire(100)).isTrue();
    assertThat(limiter.tryAcquire(1)).isFalse();
    assertThat(limiter.tryAcquire(101)).isFalse();

    await()
        .atMost(Duration.ofSeconds(2))
        .pollInterval(Duration.ofMillis(5))
        .until(() -> limiter.availableTokens() >= 20.0);

    assertThat(limiter.tryAcquire(20)).isTrue();
  }

  @Test
  @DisplayName("permits > capacity отклоняется, не расходуя токены")
  void permitsAboveCapacityDoesNotConsumeAnything() {
    Kata03_TokenBucketRateLimiter limiter = newLimiter(5, 0.001);

    assertThat(limiter.tryAcquire(6)).isFalse();
    assertThat(limiter.availableTokens()).isBetween(4.9, 5.0);

    for (int i = 0; i < 5; i++) {
      assertThat(limiter.tryAcquire()).isTrue();
    }
    assertThat(limiter.tryAcquire()).isFalse();
  }

  @Test
  @DisplayName("Конкурентный burst: суммарно проходит не больше capacity токенов")
  void concurrentBurstNeverExceedsCapacity() throws Exception {
    int capacity = 100;
    int threads = 8;
    int attemptsPerThread = 50;
    // Пополнение практически нулевое, чтобы результат был детерминированным.
    Kata03_TokenBucketRateLimiter limiter = newLimiter(capacity, 0.000001);

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger granted = new AtomicInteger();
    try {
      List<Future<?>> tasks = new ArrayList<>();
      for (int t = 0; t < threads; t++) {
        tasks.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  for (int i = 0; i < attemptsPerThread; i++) {
                    if (limiter.tryAcquire()) {
                      granted.incrementAndGet();
                    }
                  }
                  return null;
                }));
      }
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      for (Future<?> task : tasks) {
        task.get(10, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(granted).hasValue(capacity);
  }

  @Test
  @DisplayName("Длительный простой не создаёт burst больше capacity")
  void idleBucketIsCappedAtCapacity() throws InterruptedException {
    Kata03_TokenBucketRateLimiter limiter = newLimiter(10, 1000.0);
    assertThat(limiter.tryAcquire(10)).isTrue();

    Thread.sleep(60); // достаточно для начисления десятков токенов

    assertThat(limiter.availableTokens()).isEqualTo(10.0);
    assertThat(limiter.tryAcquire(10)).isTrue();
    assertThat(limiter.tryAcquire(1)).isFalse();
  }

  @Test
  @DisplayName("Валидация permits и конфигурации")
  void validation() {
    Kata03_TokenBucketRateLimiter limiter = newLimiter(10, 1.0);

    assertThatThrownBy(() -> limiter.tryAcquire(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> limiter.tryAcquire(-1)).isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> newLimiter(0, 1.0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newLimiter(-5, 1.0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newLimiter(10, 0.0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newLimiter(10, -1.0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newLimiter(10, Double.NaN))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newLimiter(10, Double.POSITIVE_INFINITY))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
