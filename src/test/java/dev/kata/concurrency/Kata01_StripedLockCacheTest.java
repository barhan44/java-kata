package dev.kata.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import dev.kata.solutions.concurrency.Kata01_StripedLockCacheSolution;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 01.
 *
 * <p>По умолчанию проверяют заготовку {@link Kata01_StripedLockCache} (и падают с {@code
 * UnsupportedOperationException}). Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata01* -DuseSolution=true</pre>
 */
@DisplayName("Kata 01: потокобезопасный кэш с lock striping")
class Kata01_StripedLockCacheTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private <K, V> Kata01_StripedLockCache<K, V> newCache(int stripes, long ttlMillis) {
    return USE_SOLUTION
        ? new Kata01_StripedLockCacheSolution<>(stripes, ttlMillis)
        : new Kata01_StripedLockCache<>(stripes, ttlMillis);
  }

  @Test
  @DisplayName("put + getIfPresent: значение видно, статистика hit/miss корректна")
  void putAndGetIfPresent() {
    Kata01_StripedLockCache<String, Integer> cache = newCache(8, 60_000);

    cache.put("answer", 42);

    assertThat(cache.getIfPresent("answer")).contains(42);
    assertThat(cache.getIfPresent("missing")).isEmpty();
    assertThat(cache.hitCount()).isEqualTo(1);
    assertThat(cache.missCount()).isEqualTo(1);
    assertThat(cache.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("get: загрузчик вызывается один раз, повторное чтение идёт из кэша")
  void getLoadsValueOnlyOnce() {
    Kata01_StripedLockCache<String, Integer> cache = newCache(8, 60_000);
    AtomicInteger loads = new AtomicInteger();

    Integer first = cache.get("key", k -> loads.incrementAndGet());
    Integer second = cache.get("key", k -> loads.incrementAndGet());

    assertThat(first).isEqualTo(1);
    assertThat(second).isEqualTo(1);
    assertThat(loads).hasValue(1);
    assertThat(cache.hitCount()).isEqualTo(1);
    assertThat(cache.missCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("TTL: просроченная запись исчезает и загружается заново")
  void expiredEntryIsReloaded() {
    Kata01_StripedLockCache<String, Integer> cache = newCache(8, 40);
    AtomicInteger loads = new AtomicInteger();

    assertThat(cache.get("key", k -> loads.incrementAndGet())).isEqualTo(1);
    assertThat(cache.getIfPresent("key")).contains(1);

    // Фоновых потоков нет — ждём, пока ленивая проверка при доступе увидит истечение TTL.
    await()
        .atMost(Duration.ofSeconds(2))
        .pollInterval(Duration.ofMillis(10))
        .until(() -> cache.getIfPresent("key").isEmpty());

    assertThat(cache.get("key", k -> loads.incrementAndGet())).isEqualTo(2);
    assertThat(loads).hasValue(2);
  }

  @Test
  @DisplayName("Cache stampede: 32 потока на один ключ вызывают загрузчик ровно один раз")
  void stampedeProtection() throws Exception {
    Kata01_StripedLockCache<String, Integer> cache = newCache(16, 60_000);
    AtomicInteger loads = new AtomicInteger();
    int threads = 32;

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Integer>> futures = new ArrayList<>(threads);
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return cache.get(
                      "hot-key",
                      k -> {
                        loads.incrementAndGet();
                        sleep(50); // эмулируем дорогой загрузчик
                        return 7;
                      });
                }));
      }

      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      for (Future<Integer> future : futures) {
        assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo(7);
      }
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(loads).hasValue(1);
    assertThat(cache.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("Конкурентная запись/чтение разных ключей не теряет данные")
  void concurrentDistinctKeys() throws Exception {
    Kata01_StripedLockCache<Integer, Integer> cache = newCache(32, 60_000);
    int threads = 8;
    int perThread = 200;

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int t = 0; t < threads; t++) {
        int threadId = t;
        futures.add(
            pool.submit(
                () -> {
                  for (int i = 0; i < perThread; i++) {
                    int key = threadId * perThread + i;
                    cache.put(key, key * 2);
                    assertThat(cache.getIfPresent(key)).contains(key * 2);
                  }
                }));
      }
      for (Future<?> future : futures) {
        future.get(10, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(cache.size()).isEqualTo(threads * perThread);
  }

  @Test
  @DisplayName("remove/clear: удаление возвращает признак живой записи")
  void removeAndClear() {
    Kata01_StripedLockCache<String, String> cache = newCache(4, 60_000);

    cache.put("a", "A");
    assertThat(cache.remove("a")).isTrue();
    assertThat(cache.remove("a")).isFalse();

    cache.put("b", "B");
    cache.put("c", "C");
    assertThat(cache.size()).isEqualTo(2);
    cache.clear();
    assertThat(cache.size()).isZero();
    assertThat(cache.getIfPresent("b")).isEqualTo(Optional.empty());
  }

  @Test
  @DisplayName("size: не учитывает просроченные записи")
  void sizeExcludesExpired() {
    Kata01_StripedLockCache<String, String> cache = newCache(4, 30);
    cache.put("a", "A");
    cache.put("b", "B");
    cache.put("c", "C");
    assertThat(cache.size()).isEqualTo(3);

    await()
        .atMost(Duration.ofSeconds(2))
        .pollInterval(Duration.ofMillis(10))
        .until(() -> cache.size() == 0);
  }

  @Test
  @DisplayName("remove: просроченная запись не считается удалённой")
  void removeExpiredReturnsFalse() throws InterruptedException {
    Kata01_StripedLockCache<String, String> cache = newCache(4, 20);
    cache.put("a", "A");
    Thread.sleep(80); // заведомо больше TTL

    assertThat(cache.remove("a")).isFalse();
  }

  @Test
  @DisplayName("null-ключи и null-значения запрещены")
  void nullsRejected() {
    Kata01_StripedLockCache<String, String> cache = newCache(4, 60_000);

    assertThatThrownBy(() -> cache.put(null, "x")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> cache.put("x", null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> cache.get(null, k -> "x")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> cache.get("x", null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> cache.get("x", k -> null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Исключение загрузчика не ломает кэш и не удерживает блокировку")
  void loaderFailureDoesNotPoisonCache() {
    Kata01_StripedLockCache<String, String> cache = newCache(4, 60_000);
    AtomicInteger calls = new AtomicInteger();

    assertThatThrownBy(
            () ->
                cache.get(
                    "x",
                    k -> {
                      calls.incrementAndGet();
                      throw new IllegalStateException("boom");
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");

    assertThat(cache.get("x", k -> "recovered")).isEqualTo("recovered");
    assertThat(calls).hasValue(1);
    assertThat(cache.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("Валидация конфигурации: stripes > 0 и ttl > 0")
  void invalidConfiguration() {
    assertThatThrownBy(() -> newCache(0, 1000)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newCache(-1, 1000)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newCache(4, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newCache(4, -5)).isInstanceOf(IllegalArgumentException.class);
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted", e);
    }
  }
}
