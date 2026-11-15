package dev.kata.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.streams.Kata05_CustomSpliteratorSolution;
import dev.kata.streams.Kata05_CustomSpliterator.Reading;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 05.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata05* -DuseSolution=true</pre>
 */
@DisplayName("Kata 05: собственный Spliterator с ленивым парсингом")
class Kata05_CustomSpliteratorTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Spliterator<Reading> newSpliterator(List<String> lines) {
    return USE_SOLUTION
        ? new Kata05_CustomSpliteratorSolution(lines)
        : new Kata05_CustomSpliterator(lines);
  }

  private static List<String> lines(String... values) {
    return List.of(values);
  }

  @Test
  @DisplayName("Последовательный парсинг: timestamp и value извлечены корректно")
  void parsesSequentially() {
    Spliterator<Reading> spliterator = newSpliterator(lines("1000;1.5", "1001;2.5", "1002;3.5"));

    List<Reading> readings = StreamSupport.stream(spliterator, false).toList();

    assertThat(readings).extracting(Reading::timestamp).containsExactly(1000L, 1001L, 1002L);
    assertThat(readings).extracting(Reading::value).containsExactly(1.5, 2.5, 3.5);
  }

  @Test
  @DisplayName("Пустые строки и комментарии пропускаются")
  void skipsBlankLinesAndComments() {
    Spliterator<Reading> spliterator =
        newSpliterator(lines("# header", "", "   ", "10; 1.0", "11;2.0", "# footer", " "));

    List<Reading> readings = StreamSupport.stream(spliterator, false).toList();

    assertThat(readings).extracting(Reading::timestamp).containsExactly(10L, 11L);
  }

  @Test
  @DisplayName("Некорректная строка: сообщение содержит номер строки и содержимое")
  void invalidLineReportsLineNumber() {
    Spliterator<Reading> spliterator = newSpliterator(lines("1;2.0", "oops"));

    assertThatThrownBy(() -> StreamSupport.stream(spliterator, false).toList())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("#2")
        .hasMessageContaining("oops");
  }

  @Test
  @DisplayName("Некорректные timestamp/value/разделитель отклоняются")
  void invalidFormatsAreRejected() {
    assertThatThrownBy(() -> drain(lines("abc;1.0")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("timestamp");
    assertThatThrownBy(() -> drain(lines("1;abc")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("value");
    assertThatThrownBy(() -> drain(lines("1;NaN")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("finite");
    assertThatThrownBy(() -> drain(lines("1;Infinity")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("finite");
    assertThatThrownBy(() -> drain(lines("42")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("separator");
    assertThatThrownBy(() -> drain(lines(";1.0")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty");
  }

  @Test
  @DisplayName("Пустой вход: нет элементов, trySplit возвращает null")
  void emptyInput() {
    Spliterator<Reading> spliterator = newSpliterator(List.of());

    assertThat(spliterator.estimateSize()).isZero();
    assertThat(spliterator.trySplit()).isNull();
    assertThat(StreamSupport.stream(spliterator, false).count()).isZero();
  }

  @Test
  @DisplayName("Один элемент: trySplit возвращает null, tryAdvance срабатывает один раз")
  void singleElement() {
    Spliterator<Reading> spliterator = newSpliterator(lines("7;8.0"));

    assertThat(spliterator.trySplit()).isNull();
    assertThat(spliterator.estimateSize()).isEqualTo(1);

    AtomicInteger seen = new AtomicInteger();
    assertThat(spliterator.tryAdvance(reading -> seen.incrementAndGet())).isTrue();
    assertThat(spliterator.tryAdvance(reading -> seen.incrementAndGet())).isFalse();
    assertThat(seen).hasValue(1);
  }

  @Test
  @DisplayName("trySplit делит диапазон пополам без потери элементов")
  void trySplitHalvesRange() {
    List<String> source = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      source.add(i + ";1.0");
    }
    Spliterator<Reading> spliterator = newSpliterator(source);
    assertThat(spliterator.estimateSize()).isEqualTo(100);

    Spliterator<Reading> prefix = spliterator.trySplit();

    assertThat(prefix).isNotNull();
    assertThat(prefix.estimateSize()).isEqualTo(50);
    assertThat(spliterator.estimateSize()).isEqualTo(50);

    long prefixCount = StreamSupport.stream(prefix, false).count();
    long suffixCount = StreamSupport.stream(spliterator, false).count();
    assertThat(prefixCount + suffixCount).isEqualTo(100);
  }

  @Test
  @DisplayName("Параллельный стрим даёт тот же результат, что и последовательный")
  void parallelStreamMatchesSequential() {
    List<String> source = new ArrayList<>();
    for (int i = 0; i < 10_000; i++) {
      source.add(i + ";" + (i % 7 + 1) + ".0");
    }

    double sequential =
        StreamSupport.stream(newSpliterator(source), false).mapToDouble(Reading::value).sum();
    long parallelCount =
        StreamSupport.stream(newSpliterator(source), true).mapToDouble(Reading::value).count();
    double parallel =
        StreamSupport.stream(newSpliterator(source), true).mapToDouble(Reading::value).sum();

    assertThat(parallelCount).isEqualTo(10_000);
    assertThat(parallel).isEqualTo(sequential);
  }

  @Test
  @DisplayName("Ленивость: limit(2) читает только первые строки, а не весь список")
  void parsingIsLazy() {
    List<String> source = IntStream.range(0, 1_000).mapToObj(i -> i + ";1.0").toList();
    CountingList counting = new CountingList(source);
    Spliterator<Reading> spliterator = newSpliterator(counting);

    List<Reading> firstTwo = StreamSupport.stream(spliterator, false).limit(2).toList();

    assertThat(firstTwo).hasSize(2);
    assertThat(counting.gets()).isLessThanOrEqualTo(3);
  }

  @Test
  @DisplayName("Характеристики: ORDERED, NONNULL, IMMUTABLE и без SIZED")
  void characteristics() {
    Spliterator<Reading> spliterator = newSpliterator(lines("1;1.0"));

    assertThat(spliterator.hasCharacteristics(Spliterator.ORDERED)).isTrue();
    assertThat(spliterator.hasCharacteristics(Spliterator.NONNULL)).isTrue();
    assertThat(spliterator.hasCharacteristics(Spliterator.IMMUTABLE)).isTrue();
    assertThat(spliterator.hasCharacteristics(Spliterator.SIZED)).isFalse();
  }

  @Test
  @DisplayName("null action и null список отклоняются")
  void nullsRejected() {
    Spliterator<Reading> spliterator = newSpliterator(lines("1;1.0"));
    assertThatThrownBy(() -> spliterator.tryAdvance(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSpliterator(null)).isInstanceOf(NullPointerException.class);
  }

  private static List<Reading> drain(List<String> source) {
    return StreamSupport.stream(
            USE_SOLUTION
                ? new Kata05_CustomSpliteratorSolution(source)
                : new Kata05_CustomSpliterator(source),
            false)
        .toList();
  }

  /** Список, считающий обращения к элементам — для проверки ленивости. */
  private static final class CountingList extends AbstractList<String> {
    private final List<String> delegate;
    private final AtomicInteger gets = new AtomicInteger();

    private CountingList(List<String> delegate) {
      this.delegate = delegate;
    }

    @Override
    public String get(int index) {
      gets.incrementAndGet();
      return delegate.get(index);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    private int gets() {
      return gets.get();
    }
  }
}
