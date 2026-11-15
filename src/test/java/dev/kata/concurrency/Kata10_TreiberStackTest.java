package dev.kata.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.concurrency.Kata10_TreiberStackSolution;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 10.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata10* -DuseSolution=true</pre>
 */
@DisplayName("Kata 10: lock-free стек Трайбера с защитой от ABA")
class Kata10_TreiberStackTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private <T> Kata10_TreiberStack<T> newStack() {
    return USE_SOLUTION ? new Kata10_TreiberStackSolution<>() : new Kata10_TreiberStack<>();
  }

  @Test
  @DisplayName("push/pop соблюдают LIFO")
  void lifoOrder() {
    Kata10_TreiberStack<Integer> stack = newStack();

    stack.push(1);
    stack.push(2);
    stack.push(3);

    assertThat(stack.pop()).contains(3);
    assertThat(stack.pop()).contains(2);
    assertThat(stack.pop()).contains(1);
    assertThat(stack.pop()).isEmpty();
  }

  @Test
  @DisplayName("Пустой стек: pop/peek возвращают empty, isEmpty истинно")
  void emptyStack() {
    Kata10_TreiberStack<String> stack = newStack();

    assertThat(stack.pop()).isEmpty();
    assertThat(stack.peek()).isEmpty();
    assertThat(stack.isEmpty()).isTrue();
    assertThat(stack.size()).isZero();
  }

  @Test
  @DisplayName("peek не удаляет элемент, size отражает изменения")
  void peekAndSize() {
    Kata10_TreiberStack<String> stack = newStack();

    stack.push("a");
    stack.push("b");

    assertThat(stack.peek()).contains("b");
    assertThat(stack.size()).isEqualTo(2);
    assertThat(stack.isEmpty()).isFalse();
    assertThat(stack.pop()).contains("b");
    assertThat(stack.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("push(null) запрещён")
  void nullIsRejected() {
    Kata10_TreiberStack<String> stack = newStack();
    assertThatThrownBy(() -> stack.push(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Один поток: 10 000 push, затем 10 000 pop в обратном порядке")
  void singleThreadedStress() {
    Kata10_TreiberStack<Integer> stack = newStack();
    int count = 10_000;

    for (int i = 0; i < count; i++) {
      stack.push(i);
    }
    assertThat(stack.size()).isEqualTo(count);

    for (int i = count - 1; i >= 0; i--) {
      assertThat(stack.pop()).contains(i);
    }
    assertThat(stack.isEmpty()).isTrue();
    assertThat(stack.size()).isZero();
  }

  @Test
  @DisplayName("Стресс: 8 продюсеров и 8 консьюмеров не теряют и не дублируют элементы")
  void concurrentProducersAndConsumersPreserveEveryItem() throws Exception {
    Kata10_TreiberStack<Integer> stack = newStack();
    int producers = 8;
    int consumers = 8;
    int perProducer = 12_500;
    int total = producers * perProducer;

    Set<Integer> poppedValues = ConcurrentHashMap.newKeySet();
    LongAdder poppedSum = new LongAdder();
    AtomicInteger remaining = new AtomicInteger(total);
    AtomicBoolean duplicate = new AtomicBoolean();
    CountDownLatch start = new CountDownLatch(1);

    ExecutorService pool = Executors.newFixedThreadPool(producers + consumers);
    try {
      List<Future<?>> futures = new ArrayList<>(producers + consumers);

      for (int producer = 0; producer < producers; producer++) {
        int base = producer * perProducer;
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  for (int i = 0; i < perProducer; i++) {
                    stack.push(base + i + 1);
                  }
                  return null;
                }));
      }

      for (int consumer = 0; consumer < consumers; consumer++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  while (remaining.get() > 0) {
                    Optional<Integer> value = stack.pop();
                    if (value.isPresent()) {
                      if (!poppedValues.add(value.get())) {
                        duplicate.set(true);
                      }
                      poppedSum.add(value.get());
                      remaining.decrementAndGet();
                    } else {
                      Thread.onSpinWait();
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

    long expectedSum = (long) total * (total + 1) / 2;
    assertThat(duplicate).isFalse();
    assertThat(poppedValues).hasSize(total);
    assertThat(poppedSum.sum()).isEqualTo(expectedSum);
    assertThat(stack.isEmpty()).isTrue();
    assertThat(stack.size()).isZero();
  }

  @Test
  @DisplayName("Конкурирующие pop на пустом стеке не залипают и не возвращают null")
  void concurrentPopsOnEmptyStack() throws Exception {
    Kata10_TreiberStack<Integer> stack = newStack();
    int threads = 16;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Optional<Integer>>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return stack.pop();
                }));
      }
      start.countDown();
      for (Future<Optional<Integer>> future : futures) {
        assertThat(future.get(5, TimeUnit.SECONDS)).isEmpty();
      }
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
    assertThat(stack.isEmpty()).isTrue();
  }
}
