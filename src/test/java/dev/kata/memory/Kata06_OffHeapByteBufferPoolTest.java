package dev.kata.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import dev.kata.solutions.memory.Kata06_OffHeapByteBufferPoolSolution;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 06.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata06* -DuseSolution=true</pre>
 */
@DisplayName("Kata 06: пул off-heap буферов с Cleaner-защитой")
class Kata06_OffHeapByteBufferPoolTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  /** Prevent JIT from eliminating garbage allocations used to provoke GC. */
  private static volatile Object sink;

  private Kata06_OffHeapByteBufferPool newPool(int bufferSize, int maxPooled) {
    return USE_SOLUTION
        ? new Kata06_OffHeapByteBufferPoolSolution(bufferSize, maxPooled)
        : new Kata06_OffHeapByteBufferPool(bufferSize, maxPooled);
  }

  @Test
  @DisplayName("acquire выдаёт direct-буфер нужного размера и учитывает его inUse")
  void acquireReturnsDirectBuffer() {
    Kata06_OffHeapByteBufferPool pool = newPool(256, 4);
    try (Kata06_OffHeapByteBufferPool.Lease lease = pool.acquire()) {
      ByteBuffer buffer = lease.buffer();
      assertThat(buffer.isDirect()).isTrue();
      assertThat(buffer.capacity()).isEqualTo(256);
      assertThat(pool.inUseCount()).isEqualTo(1);
      assertThat(pool.pooledCount()).isZero();
    }
    assertThat(pool.inUseCount()).isZero();
    assertThat(pool.pooledCount()).isEqualTo(1);
    assertThat(pool.leakCount()).isZero();
  }

  @Test
  @DisplayName("Закрытый lease возвращает тот же буфер в пул — повторное использование")
  void bufferIsReusedAfterClose() {
    Kata06_OffHeapByteBufferPool pool = newPool(128, 4);

    Kata06_OffHeapByteBufferPool.Lease first = pool.acquire();
    ByteBuffer original = first.buffer();
    first.close();

    try (Kata06_OffHeapByteBufferPool.Lease second = pool.acquire()) {
      assertThat(second.buffer()).isSameAs(original);
      assertThat(second.buffer().position()).isZero();
    }
  }

  @Test
  @DisplayName("Пул не хранит больше maxPooledBuffers буферов")
  void poolRespectsMaxSize() {
    Kata06_OffHeapByteBufferPool pool = newPool(64, 2);
    List<Kata06_OffHeapByteBufferPool.Lease> leases = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      leases.add(pool.acquire());
    }
    assertThat(pool.inUseCount()).isEqualTo(5);

    leases.forEach(Kata06_OffHeapByteBufferPool.Lease::close);

    assertThat(pool.inUseCount()).isZero();
    assertThat(pool.pooledCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("Двойной close безопасен и не кладёт буфер в пул дважды")
  void doubleCloseIsIdempotent() {
    Kata06_OffHeapByteBufferPool pool = newPool(64, 4);
    Kata06_OffHeapByteBufferPool.Lease lease = pool.acquire();

    lease.close();
    lease.close();

    assertThat(pool.pooledCount()).isEqualTo(1);
    assertThat(pool.inUseCount()).isZero();
  }

  @Test
  @DisplayName("После close буфер недоступен")
  void bufferIsUnavailableAfterClose() {
    Kata06_OffHeapByteBufferPool pool = newPool(64, 4);
    Kata06_OffHeapByteBufferPool.Lease lease = pool.acquire();
    lease.close();

    assertThatThrownBy(lease::buffer)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("closed");
  }

  @Test
  @DisplayName("Закрытый пул: acquire падает, close пула идемпотентен")
  void closedPoolRejectsAcquire() {
    Kata06_OffHeapByteBufferPool pool = newPool(64, 4);
    try (Kata06_OffHeapByteBufferPool.Lease lease = pool.acquire()) {
      assertThat(lease.buffer().capacity()).isEqualTo(64);
    }
    assertThat(pool.pooledCount()).isEqualTo(1);

    pool.close();
    pool.close();

    assertThat(pool.pooledCount()).isZero();
    assertThatThrownBy(pool::acquire).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Lease, выданный до закрытия пула, можно закрыть после")
  void outstandingLeaseCanBeClosedAfterPoolClose() {
    Kata06_OffHeapByteBufferPool pool = newPool(64, 4);
    Kata06_OffHeapByteBufferPool.Lease lease = pool.acquire();

    pool.close();
    lease.close();

    assertThat(pool.inUseCount()).isZero();
    assertThat(pool.pooledCount()).isZero(); // в закрытый пул не возвращаем
  }

  @Test
  @DisplayName("Конкурентные acquire/close не теряют и не дублируют буферы")
  void concurrentAcquireAndRelease() throws Exception {
    Kata06_OffHeapByteBufferPool pool = newPool(1024, 8);
    int threads = 8;
    int iterations = 300;

    ExecutorService executor = Executors.newFixedThreadPool(threads);
    try {
      List<Future<?>> tasks = new ArrayList<>();
      for (int t = 0; t < threads; t++) {
        tasks.add(
            executor.submit(
                () -> {
                  for (int i = 0; i < iterations; i++) {
                    try (Kata06_OffHeapByteBufferPool.Lease lease = pool.acquire()) {
                      ByteBuffer buffer = lease.buffer();
                      buffer.putLong(0, i);
                      assertThat(buffer.getLong(0)).isEqualTo(i);
                    }
                  }
                  return null;
                }));
      }
      for (Future<?> task : tasks) {
        task.get(30, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(pool.inUseCount()).isZero();
    assertThat(pool.pooledCount()).isBetween(0, 8);
    assertThat(pool.leakCount()).isZero();
  }

  @Test
  @DisplayName("Lease, забытый без close, возвращается Cleaner'ом и считается утечкой")
  void abandonedLeaseIsReclaimedByCleaner() {
    Kata06_OffHeapByteBufferPool pool = newPool(64, 4);

    abandonLease(pool);

    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(100))
        .until(
            () -> {
              // Помогаем GC увидеть мусор и запустить Cleaner.
              for (int i = 0; i < 1_000; i++) {
                sink = new byte[1024];
              }
              System.gc();
              return pool.leakCount() >= 1;
            });

    await().atMost(Duration.ofSeconds(5)).until(() -> pool.inUseCount() == 0);
    assertThat(pool.pooledCount()).isBetween(0, 4);
  }

  @Test
  @DisplayName("Валидация размеров")
  void validation() {
    assertThatThrownBy(() -> newPool(0, 4)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newPool(-1, 4)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newPool(64, -1)).isInstanceOf(IllegalArgumentException.class);
  }

  /** Отдельный метод: после его возврата lease недостижим для GC. */
  private static void abandonLease(Kata06_OffHeapByteBufferPool pool) {
    pool.acquire();
  }
}
