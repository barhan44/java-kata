package dev.kata.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.memory.Kata14_ArenaSlabAllocatorSolution;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 14.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata14* -DuseSolution=true</pre>
 */
@DisplayName("Kata 14: off-heap slab allocator поверх MemorySegment")
class Kata14_ArenaSlabAllocatorTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata14_ArenaSlabAllocator newAllocator(int slotSizeBytes, int slotCount) {
    return USE_SOLUTION
        ? new Kata14_ArenaSlabAllocatorSolution(slotSizeBytes, slotCount)
        : new Kata14_ArenaSlabAllocator(slotSizeBytes, slotCount);
  }

  @Test
  @DisplayName("Запись и чтение примитивов через срез слота")
  void writeAndReadPrimitives() {
    try (Kata14_ArenaSlabAllocator allocator = newAllocator(32, 2)) {
      long slot = allocator.allocate();
      assertThat(slot).isZero();

      MemorySegment slice = allocator.slice(slot);
      slice.set(ValueLayout.JAVA_LONG, 0, 1234567890123L);
      slice.set(ValueLayout.JAVA_INT, 8, 42);
      slice.set(ValueLayout.JAVA_DOUBLE, 16, 3.14159);

      assertThat(slice.get(ValueLayout.JAVA_LONG, 0)).isEqualTo(1234567890123L);
      assertThat(slice.get(ValueLayout.JAVA_INT, 8)).isEqualTo(42);
      assertThat(slice.get(ValueLayout.JAVA_DOUBLE, 16)).isEqualTo(3.14159);
      assertThat(slice.byteSize()).isEqualTo(32);
    }
  }

  @Test
  @DisplayName("Слоты изолированы, освобождённый слот переиспользуется по LIFO")
  void slotsAreIsolatedAndReused() {
    try (Kata14_ArenaSlabAllocator allocator = newAllocator(16, 3)) {
      long first = allocator.allocate();
      long second = allocator.allocate();
      assertThat(first).isZero();
      assertThat(second).isEqualTo(1);

      allocator.slice(first).set(ValueLayout.JAVA_LONG, 0, 111L);
      allocator.slice(second).set(ValueLayout.JAVA_LONG, 0, 222L);

      allocator.free(first);
      long reused = allocator.allocate();
      assertThat(reused).isEqualTo(first);

      allocator.slice(reused).set(ValueLayout.JAVA_LONG, 0, 333L);
      assertThat(allocator.slice(second).get(ValueLayout.JAVA_LONG, 0)).isEqualTo(222L);
      assertThat(allocator.slice(first).get(ValueLayout.JAVA_LONG, 0)).isEqualTo(333L);
    }
  }

  @Test
  @DisplayName("Исчерпание памяти возвращает -1, после free слот снова доступен")
  void exhaustionAndReuse() {
    try (Kata14_ArenaSlabAllocator allocator = newAllocator(8, 2)) {
      long first = allocator.allocate();
      long second = allocator.allocate();
      assertThat(first).isNotEqualTo(second);
      assertThat(allocator.allocate()).isEqualTo(-1L);

      allocator.free(second);
      assertThat(allocator.allocate()).isEqualTo(second);
      assertThat(allocator.allocate()).isEqualTo(-1L);

      allocator.free(first);
      assertThat(allocator.allocate()).isEqualTo(first);
    }
  }

  @Test
  @DisplayName("Double-free и освобождение невыделенного слота запрещены")
  void doubleFreeIsRejected() {
    try (Kata14_ArenaSlabAllocator allocator = newAllocator(8, 2)) {
      long slot = allocator.allocate();
      allocator.free(slot);

      assertThatThrownBy(() -> allocator.free(slot)).isInstanceOf(IllegalStateException.class);

      long neverAllocated = slot == 0 ? 1 : 0;
      assertThatThrownBy(() -> allocator.free(neverAllocated))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  @DisplayName("Индексы вне диапазона отклоняются")
  void outOfRangeSlotsAreRejected() {
    try (Kata14_ArenaSlabAllocator allocator = newAllocator(8, 2)) {
      assertThatThrownBy(() -> allocator.free(-1)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> allocator.free(2)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> allocator.slice(-1)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> allocator.slice(2)).isInstanceOf(IndexOutOfBoundsException.class);
    }
  }

  @Test
  @DisplayName("slice для свободного слота запрещён")
  void sliceOfFreeSlotIsRejected() {
    try (Kata14_ArenaSlabAllocator allocator = newAllocator(8, 1)) {
      long slot = allocator.allocate();
      allocator.free(slot);

      assertThatThrownBy(() -> allocator.slice(slot)).isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  @DisplayName("Валидация: размер слота кратен 8, количество положительно")
  void invalidConfiguration() {
    assertThatThrownBy(() -> newAllocator(0, 10)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newAllocator(7, 10)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newAllocator(15, 10)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newAllocator(8, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newAllocator(8, -1)).isInstanceOf(IllegalArgumentException.class);

    try (Kata14_ArenaSlabAllocator valid = newAllocator(8, 1)) {
      assertThat(valid.allocate()).isGreaterThanOrEqualTo(0L);
    }
  }

  @Test
  @DisplayName("close идемпотентен, после закрытия операции запрещены")
  void closeIsIdempotentAndBlocksOperations() {
    Kata14_ArenaSlabAllocator allocator = newAllocator(8, 1);
    long slot = allocator.allocate();

    allocator.close();
    allocator.close();

    assertThatThrownBy(allocator::allocate).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> allocator.free(slot)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> allocator.slice(slot)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Стресс: слоты не выдаются двум потокам одновременно")
  void concurrentAllocateFreeStress() throws Exception {
    int slots = 256;
    int threads = 8;
    int iterationsPerThread = 20_000;

    try (Kata14_ArenaSlabAllocator allocator = newAllocator(16, slots)) {
      Set<Long> inUse = ConcurrentHashMap.newKeySet();
      AtomicBoolean duplicate = new AtomicBoolean();
      CountDownLatch start = new CountDownLatch(1);
      ExecutorService pool = Executors.newFixedThreadPool(threads);
      try {
        List<Future<?>> futures = new ArrayList<>();
        for (int thread = 0; thread < threads; thread++) {
          int threadId = thread;
          futures.add(
              pool.submit(
                  () -> {
                    start.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                      long slot;
                      while ((slot = allocator.allocate()) < 0L) {
                        Thread.onSpinWait();
                      }
                      if (!inUse.add(slot)) {
                        duplicate.set(true);
                      }
                      try {
                        MemorySegment slice = allocator.slice(slot);
                        long value = slot * 31L + threadId + i;
                        slice.set(ValueLayout.JAVA_LONG, 0, value);
                        if (slice.get(ValueLayout.JAVA_LONG, 0) != value) {
                          throw new AssertionError("slot content was corrupted");
                        }
                      } finally {
                        // Снимаем отметку до free: иначе следующий владелец слота
                        // может добавить его в set раньше, чем мы удалим старую запись.
                        inUse.remove(slot);
                        allocator.free(slot);
                      }
                    }
                    return null;
                  }));
        }

        start.countDown();
        for (Future<?> future : futures) {
          future.get(60, TimeUnit.SECONDS);
        }
      } finally {
        pool.shutdownNow();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
      }

      assertThat(duplicate).isFalse();
      assertThat(inUse).isEmpty();

      Set<Long> all = new HashSet<>();
      for (int i = 0; i < slots; i++) {
        long slot = allocator.allocate();
        assertThat(slot).isGreaterThanOrEqualTo(0L);
        assertThat(all.add(slot)).isTrue();
      }
      assertThat(allocator.allocate()).isEqualTo(-1L);
      for (long slot : all) {
        allocator.free(slot);
      }
    }
  }
}
