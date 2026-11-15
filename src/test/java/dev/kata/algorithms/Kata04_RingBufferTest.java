package dev.kata.algorithms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.algorithms.Kata04_RingBufferSolution;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 04.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata04* -DuseSolution=true</pre>
 */
@DisplayName("Kata 04: lock-free SPSC ring buffer")
class Kata04_RingBufferTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private <T> Kata04_RingBuffer<T> newBuffer(int capacity) {
    return USE_SOLUTION
        ? new Kata04_RingBufferSolution<>(capacity)
        : new Kata04_RingBuffer<>(capacity);
  }

  @Test
  @DisplayName("FIFO: элементы возвращаются в порядке добавления")
  void fifoOrder() {
    Kata04_RingBuffer<Integer> buffer = newBuffer(4);

    assertThat(buffer.offer(1)).isTrue();
    assertThat(buffer.offer(2)).isTrue();
    assertThat(buffer.offer(3)).isTrue();
    assertThat(buffer.size()).isEqualTo(3);
    assertThat(buffer.isEmpty()).isFalse();

    assertThat(buffer.poll()).isEqualTo(1);
    assertThat(buffer.poll()).isEqualTo(2);
    assertThat(buffer.poll()).isEqualTo(3);
    assertThat(buffer.poll()).isNull();
    assertThat(buffer.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("Полный буфер: offer отклоняется, пока consumer не освободит слот")
  void fullBufferRejectsOffer() {
    Kata04_RingBuffer<String> buffer = newBuffer(2);

    assertThat(buffer.offer("a")).isTrue();
    assertThat(buffer.offer("b")).isTrue();
    assertThat(buffer.offer("c")).isFalse();
    assertThat(buffer.size()).isEqualTo(2);

    assertThat(buffer.poll()).isEqualTo("a");
    assertThat(buffer.offer("c")).isTrue();
    assertThat(buffer.poll()).isEqualTo("b");
    assertThat(buffer.poll()).isEqualTo("c");
  }

  @Test
  @DisplayName("Многократный wrap-around сохраняет порядок")
  void wrapAroundKeepsOrder() {
    Kata04_RingBuffer<Integer> buffer = newBuffer(4);

    for (int round = 0; round < 10; round++) {
      for (int i = 0; i < 4; i++) {
        assertThat(buffer.offer(round * 10 + i)).isTrue();
      }
      assertThat(buffer.offer(-1)).isFalse();
      for (int i = 0; i < 4; i++) {
        assertThat(buffer.poll()).isEqualTo(round * 10 + i);
      }
      assertThat(buffer.isEmpty()).isTrue();
    }
  }

  @Test
  @DisplayName("Ёмкость 1 — минимальный корректный размер")
  void capacityOne() {
    Kata04_RingBuffer<String> buffer = newBuffer(1);

    assertThat(buffer.offer("x")).isTrue();
    assertThat(buffer.offer("y")).isFalse();
    assertThat(buffer.poll()).isEqualTo("x");
    assertThat(buffer.poll()).isNull();
    assertThat(buffer.offer("y")).isTrue();
    assertThat(buffer.poll()).isEqualTo("y");
  }

  @Test
  @DisplayName("null не поддерживается")
  void nullIsRejected() {
    Kata04_RingBuffer<String> buffer = newBuffer(4);
    assertThatThrownBy(() -> buffer.offer(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("SPSC: 200 000 событий проходят без потерь и перестановок")
  void spscStress() throws Exception {
    int total = 200_000;
    Kata04_RingBuffer<Long> buffer = newBuffer(1024);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Future<?> producer =
          pool.submit(
              () -> {
                start.await();
                for (long i = 0; i < total; i++) {
                  while (!buffer.offer(i)) {
                    Thread.onSpinWait();
                  }
                }
                return null;
              });

      Future<?> consumer =
          pool.submit(
              () -> {
                start.await();
                long expected = 0;
                while (expected < total) {
                  Long value = buffer.poll();
                  if (value == null) {
                    Thread.onSpinWait();
                    continue;
                  }
                  assertThat(value).isEqualTo(expected);
                  expected++;
                }
                return null;
              });

      start.countDown();
      producer.get(30, TimeUnit.SECONDS);
      consumer.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(buffer.isEmpty()).isTrue();
    assertThat(buffer.size()).isZero();
  }

  @Test
  @DisplayName("Валидация: capacity > 0 и степень двойки")
  void capacityValidation() {
    assertThatThrownBy(() -> newBuffer(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBuffer(-4)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBuffer(3)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newBuffer(6)).isInstanceOf(IllegalArgumentException.class);
  }
}
