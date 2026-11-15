package dev.kata.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.streams.Kata18_CustomStreamGathererSolution;
import dev.kata.streams.Kata18_CustomStreamGatherer.StreamGatherer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 18.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata18* -DuseSolution=true</pre>
 */
@DisplayName("Kata 18: собственные Gatherer'ы для Stream API")
class Kata18_CustomStreamGathererTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  @Test
  @DisplayName("distinctBy сохраняет порядок первых вхождений")
  void distinctByPreservesEncounterOrder() {
    assertThat(
            gather(
                    Stream.of("apple", "banana", "avocado", "blueberry", "cherry"),
                    distinctBy(value -> value.charAt(0)))
                .toList())
        .containsExactly("apple", "banana", "cherry");

    assertThat(gather(Stream.of(1, 2, 1, 3, 2, 4), distinctBy(Function.identity())).toList())
        .containsExactly(1, 2, 3, 4);
  }

  @Test
  @DisplayName("distinctBy допускает null-элементы")
  void distinctBySupportsNulls() {
    assertThat(
            gather(Stream.of("a", null, "a", null, "b"), distinctBy(Function.identity())).toList())
        .containsExactly("a", null, "b");
  }

  @Test
  @DisplayName("distinctBy ленив: limit(2) читает только два элемента")
  void distinctByIsLazy() {
    AtomicInteger consumed = new AtomicInteger();

    List<Integer> result =
        gather(
                Stream.iterate(0, i -> i + 1)
                    .limit(1_000_000)
                    .peek(i -> consumed.incrementAndGet()),
                distinctBy(Function.identity()))
            .limit(2)
            .toList();

    assertThat(result).containsExactly(0, 1);
    assertThat(consumed).hasValue(2);
  }

  @Test
  @DisplayName("Скользящее окно с шагом 1")
  void slidingWindowStepOne() {
    assertThat(gather(Stream.of(1, 2, 3, 4, 5), slidingWindow(3, 1)).toList())
        .containsExactly(List.of(1, 2, 3), List.of(2, 3, 4), List.of(3, 4, 5));
  }

  @Test
  @DisplayName("Скользящее окно с шагом 2")
  void slidingWindowStepTwo() {
    assertThat(gather(Stream.of(1, 2, 3, 4, 5), slidingWindow(3, 2)).toList())
        .containsExactly(List.of(1, 2, 3), List.of(3, 4, 5));
  }

  @Test
  @DisplayName("Скользящее окно с шагом 3 на семи элементах")
  void slidingWindowStepThree() {
    assertThat(gather(Stream.of(1, 2, 3, 4, 5, 6, 7), slidingWindow(3, 3)).toList())
        .containsExactly(List.of(1, 2, 3), List.of(4, 5, 6));
  }

  @Test
  @DisplayName("Шаг больше окна оставляет пропуски между окнами")
  void slidingWindowStepGreaterThanWindow() {
    assertThat(gather(Stream.of(1, 2, 3, 4, 5, 6, 7), slidingWindow(2, 3)).toList())
        .containsExactly(List.of(1, 2), List.of(4, 5));
  }

  @Test
  @DisplayName("Окно больше стрима и пустой стрим дают пустой результат")
  void slidingWindowTooLarge() {
    assertThat(gather(Stream.of(1, 2), slidingWindow(5, 1)).toList()).isEmpty();
    assertThat(gather(Stream.<Integer>empty(), slidingWindow(2, 1)).toList()).isEmpty();
  }

  @Test
  @DisplayName("Окна допускают null-элементы")
  void slidingWindowSupportsNulls() {
    List<List<Integer>> windows = gather(Stream.of(1, null, 3, 4), slidingWindow(2, 1)).toList();

    assertThat(windows).hasSize(3);
    assertThat(windows.get(0)).containsExactly(1, null);
    assertThat(windows.get(1)).containsExactly(null, 3);
    assertThat(windows.get(2)).containsExactly(3, 4);
  }

  @Test
  @DisplayName("Бесконечный стрим с окнами работает через limit")
  void slidingWindowOnInfiniteStream() {
    List<List<Integer>> windows =
        gather(Stream.iterate(0, i -> i + 1), slidingWindow(3, 2)).limit(3).toList();

    assertThat(windows).containsExactly(List.of(0, 1, 2), List.of(2, 3, 4), List.of(4, 5, 6));
  }

  @Test
  @DisplayName("Скользящее окно лениво: limit(1) читает ровно windowSize элементов")
  void slidingWindowIsLazy() {
    AtomicInteger consumed = new AtomicInteger();

    List<List<Integer>> windows =
        gather(
                Stream.iterate(0, i -> i + 1).limit(1_000).peek(i -> consumed.incrementAndGet()),
                slidingWindow(4, 2))
            .limit(1)
            .toList();

    assertThat(windows).containsExactly(List.of(0, 1, 2, 3));
    assertThat(consumed).hasValue(4);
  }

  @Test
  @DisplayName("Батчи по maxSize и хвост в finish")
  void batchBySize() {
    assertThat(
            gather(Stream.of(1, 2, 3, 4, 5, 6, 7), batchWithCondition(3, (batch, next) -> false))
                .toList())
        .containsExactly(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7));
  }

  @Test
  @DisplayName("Батчи по накопленному весу")
  void batchByWeightPredicate() {
    BiPredicate<List<Integer>, Integer> tooHeavy =
        (batch, next) -> batch.stream().mapToInt(Integer::intValue).sum() + next > 10;

    assertThat(gather(Stream.of(4, 5, 2, 7, 1), batchWithCondition(10, tooHeavy)).toList())
        .containsExactly(List.of(4, 5), List.of(2, 7, 1));
  }

  @Test
  @DisplayName("maxSize = 1 даёт батчи из одного элемента")
  void batchSizeOne() {
    assertThat(gather(Stream.of(1, 2, 3), batchWithCondition(1, (batch, next) -> false)).toList())
        .containsExactly(List.of(1), List.of(2), List.of(3));
  }

  @Test
  @DisplayName("Предикат всегда true отправляет каждый элемент отдельно")
  void predicateAlwaysFlushes() {
    assertThat(gather(Stream.of(1, 2, 3), batchWithCondition(10, (batch, next) -> true)).toList())
        .containsExactly(List.of(1), List.of(2), List.of(3));
  }

  @Test
  @DisplayName("Предикат видит текущий батч и следующий элемент")
  void predicateObservesCurrentBatch() {
    List<List<Integer>> observed = new ArrayList<>();

    gather(
            Stream.of(1, 2, 3, 4),
            batchWithCondition(
                3,
                (batch, next) -> {
                  observed.add(new ArrayList<>(batch));
                  return false;
                }))
        .toList();

    assertThat(observed).containsExactly(List.of(1), List.of(1, 2));
  }

  @Test
  @DisplayName("Пустой стрим не создаёт батчей")
  void emptyBatches() {
    assertThat(gather(Stream.<Integer>empty(), batchWithCondition(3, (b, n) -> false)).toList())
        .isEmpty();
  }

  @Test
  @DisplayName("Батчи допускают null-элементы")
  void batchSupportsNulls() {
    List<List<String>> batches =
        gather(Stream.of("a", null, "b", null), batchWithCondition(3, (batch, next) -> false))
            .toList();

    assertThat(batches).hasSize(2);
    assertThat(batches.get(0)).containsExactly("a", null, "b");
    assertThat(batches.get(1)).containsExactly((String) null);
  }

  @Test
  @DisplayName("Батчи ленивы: limit(1) читает ровно maxSize элементов")
  void batchIsLazy() {
    AtomicInteger consumed = new AtomicInteger();

    List<List<Integer>> batches =
        gather(
                Stream.iterate(0, i -> i + 1).limit(1_000).peek(i -> consumed.incrementAndGet()),
                batchWithCondition(4, (batch, next) -> false))
            .limit(1)
            .toList();

    assertThat(batches).containsExactly(List.of(0, 1, 2, 3));
    assertThat(consumed).hasValue(4);
  }

  @Test
  @DisplayName("finish вызывается для хвостового состояния")
  void customGathererFinish() {
    StreamGatherer<Integer, int[], Integer> sumAtEnd =
        new StreamGatherer<>() {
          @Override
          public int[] initial() {
            return new int[1];
          }

          @Override
          public boolean integrate(
              int[] state, Integer element, java.util.function.Consumer<Integer> downstream) {
            state[0] += element;
            return true;
          }

          @Override
          public void finish(int[] state, java.util.function.Consumer<Integer> downstream) {
            downstream.accept(state[0]);
          }
        };

    assertThat(gather(Stream.of(1, 2, 3), sumAtEnd).toList()).containsExactly(6);
  }

  @Test
  @DisplayName("integrate = false останавливает источник и отменяет finish")
  void shortCircuitStopsSource() {
    AtomicInteger consumed = new AtomicInteger();
    AtomicBoolean finishCalled = new AtomicBoolean();

    StreamGatherer<Integer, int[], Integer> firstTwo =
        new StreamGatherer<>() {
          @Override
          public int[] initial() {
            return new int[1];
          }

          @Override
          public boolean integrate(
              int[] state, Integer element, java.util.function.Consumer<Integer> downstream) {
            downstream.accept(element);
            state[0]++;
            return state[0] < 2;
          }

          @Override
          public void finish(int[] state, java.util.function.Consumer<Integer> downstream) {
            finishCalled.set(true);
          }
        };

    List<Integer> result =
        gather(
                Stream.iterate(1, i -> i + 1).limit(1_000).peek(i -> consumed.incrementAndGet()),
                firstTwo)
            .toList();

    assertThat(result).containsExactly(1, 2);
    assertThat(consumed).hasValue(2);
    assertThat(finishCalled).isFalse();
  }

  @Test
  @DisplayName("Закрытие результата закрывает исходный стрим")
  void closePropagatesToSource() {
    AtomicBoolean closed = new AtomicBoolean();
    Stream<Integer> source = Stream.of(1, 2, 3).onClose(() -> closed.set(true));

    Stream<Integer> result = gather(source, distinctBy(Function.identity()));
    result.close();

    assertThat(closed).isTrue();
  }

  @Test
  @DisplayName("Параллельный источник обрабатывается с сохранением порядка")
  void parallelSourcePreservesOrder() {
    assertThat(
            gather(Stream.of(1, 2, 1, 3, 2, 4).parallel(), distinctBy(Function.identity()))
                .toList())
        .containsExactly(1, 2, 3, 4);
  }

  @Test
  @DisplayName("Валидация аргументов")
  void invalidArguments() {
    assertThatThrownBy(() -> gather(null, distinctBy(Function.identity())))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> gather(Stream.of(1), null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> distinctBy(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> batchWithCondition(1, null)).isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> slidingWindow(0, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> slidingWindow(1, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> slidingWindow(-1, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> batchWithCondition(0, (batch, next) -> false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Окна и батчи неизменяемы")
  void outputListsAreImmutable() {
    List<Integer> window =
        gather(Stream.of(1, 2, 3), slidingWindow(3, 1)).findFirst().orElseThrow();
    assertThatThrownBy(() -> window.add(4)).isInstanceOf(UnsupportedOperationException.class);

    List<Integer> batch =
        gather(Stream.of(1, 2, 3), batchWithCondition(3, (b, n) -> false))
            .findFirst()
            .orElseThrow();
    assertThatThrownBy(() -> batch.add(4)).isInstanceOf(UnsupportedOperationException.class);
  }

  private static <T, R> Stream<R> gather(Stream<T> source, StreamGatherer<T, ?, R> gatherer) {
    return USE_SOLUTION
        ? Kata18_CustomStreamGathererSolution.gather(source, gatherer)
        : Kata18_CustomStreamGatherer.gather(source, gatherer);
  }

  private static <T, K> StreamGatherer<T, ?, T> distinctBy(
      Function<? super T, ? extends K> keyExtractor) {
    return USE_SOLUTION
        ? Kata18_CustomStreamGathererSolution.distinctBy(keyExtractor)
        : Kata18_CustomStreamGatherer.distinctBy(keyExtractor);
  }

  private static <T> StreamGatherer<T, ?, List<T>> slidingWindow(int windowSize, int step) {
    return USE_SOLUTION
        ? Kata18_CustomStreamGathererSolution.slidingWindow(windowSize, step)
        : Kata18_CustomStreamGatherer.slidingWindow(windowSize, step);
  }

  private static <T> StreamGatherer<T, ?, List<T>> batchWithCondition(
      int maxSize, BiPredicate<List<T>, T> shouldFlushBefore) {
    return USE_SOLUTION
        ? Kata18_CustomStreamGathererSolution.batchWithCondition(maxSize, shouldFlushBefore)
        : Kata18_CustomStreamGatherer.batchWithCondition(maxSize, shouldFlushBefore);
  }
}
