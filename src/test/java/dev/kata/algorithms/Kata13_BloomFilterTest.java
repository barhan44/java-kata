package dev.kata.algorithms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.algorithms.Kata13_BloomFilterSolution;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 13.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata13* -DuseSolution=true</pre>
 */
@DisplayName("Kata 13: вероятностный фильтр Блума")
class Kata13_BloomFilterTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private static Kata13_BloomFilter newFilter(long expectedInsertions, double p) {
    return USE_SOLUTION
        ? Kata13_BloomFilterSolution.create(expectedInsertions, p)
        : Kata13_BloomFilter.create(expectedInsertions, p);
  }

  @Test
  @DisplayName("Нет false negatives: все добавленные элементы всегда находятся")
  void noFalseNegatives() {
    Kata13_BloomFilter filter = newFilter(10_000, 0.01);
    List<String> inserted = new ArrayList<>();

    for (int i = 0; i < 10_000; i++) {
      String value = "inserted-" + i;
      inserted.add(value);
      filter.put(value);
    }

    for (String value : inserted) {
      assertThat(filter.mightContain(value)).as("элемент %s", value).isTrue();
      assertThat(filter.mightContain(value.getBytes(StandardCharsets.UTF_8))).isTrue();
    }
  }

  @Test
  @DisplayName("False positives на 50 000 элементах не превышают p с учётом 3 сигм")
  void falsePositiveRateStaysWithinConfiguredProbability() {
    long insertions = 50_000;
    double p = 0.01;
    Kata13_BloomFilter filter = newFilter(insertions, p);

    for (int i = 0; i < insertions; i++) {
      filter.put("present-" + i);
    }

    int falsePositives = 0;
    int probes = 50_000;
    for (int i = 0; i < probes; i++) {
      if (filter.mightContain("absent-" + i)) {
        falsePositives++;
      }
    }

    double observed = falsePositives / (double) probes;
    double sigma = Math.sqrt(p * (1.0 - p) / probes);
    double allowed = p + 3.0 * sigma;
    assertThat(observed)
        .as("false positive rate (%,d из %,d)", falsePositives, probes)
        .isLessThanOrEqualTo(allowed);
  }

  @Test
  @DisplayName("Размер и число хешей соответствуют формулам m = -n ln p / (ln 2)^2, k = m/n ln 2")
  void parametersAreCalculatedFromFormulas() {
    double p = 0.01;
    long n = 1_000;
    long expectedBits = 9_586; // ceil(9585.05...)
    int expectedHashes = 7; // round(9586 / 1000 * ln 2)

    Kata13_BloomFilter filter = newFilter(n, p);

    assertThat(filter.bitSize()).isEqualTo(expectedBits);
    assertThat(filter.hashFunctions()).isEqualTo(expectedHashes);
    assertThat(filter.expectedFalsePositiveProbability()).isEqualTo(p);
  }

  @Test
  @DisplayName("Малые параметры: n=1, p=0.5 дают минимальные m и k")
  void tinyFilterParameters() {
    Kata13_BloomFilter filter = newFilter(1, 0.5);

    assertThat(filter.bitSize()).isEqualTo(2);
    assertThat(filter.hashFunctions()).isEqualTo(1);
    filter.put("x");
    assertThat(filter.mightContain("x")).isTrue();
  }

  @Test
  @DisplayName("Строковая и байтовая перегрузки эквивалентны (UTF-8)")
  void stringAndByteOverloadsAreConsistent() {
    Kata13_BloomFilter filter = newFilter(100, 0.01);

    filter.put("hello");
    assertThat(filter.mightContain("hello".getBytes(StandardCharsets.UTF_8))).isTrue();

    filter.put("world".getBytes(StandardCharsets.UTF_8));
    assertThat(filter.mightContain("world")).isTrue();

    filter.put("привет");
    assertThat(filter.mightContain("привет".getBytes(StandardCharsets.UTF_8))).isTrue();
  }

  @Test
  @DisplayName("Пустые массивы и строки — валидные элементы")
  void emptyItemsAreSupported() {
    Kata13_BloomFilter filter = newFilter(10, 0.01);

    filter.put(new byte[0]);
    filter.put("");

    assertThat(filter.mightContain(new byte[0])).isTrue();
    assertThat(filter.mightContain("")).isTrue();
  }

  @Test
  @DisplayName("Повторное добавление не ломает фильтр")
  void duplicateInsertionsAreIdempotent() {
    Kata13_BloomFilter filter = newFilter(100, 0.01);

    for (int i = 0; i < 10; i++) {
      filter.put("duplicate");
    }

    assertThat(filter.mightContain("duplicate")).isTrue();
  }

  @Test
  @DisplayName("Детерминизм: одинаковые конфигурация и вставки дают одинаковые ответы")
  void filtersAreDeterministic() {
    Kata13_BloomFilter first = newFilter(1_000, 0.01);
    Kata13_BloomFilter second = newFilter(1_000, 0.01);

    for (int i = 0; i < 1_000; i++) {
      first.put("value-" + i);
      second.put("value-" + i);
    }

    for (int i = 0; i < 10_000; i++) {
      String probe = "probe-" + i;
      assertThat(first.mightContain(probe)).isEqualTo(second.mightContain(probe));
    }
  }

  @Test
  @DisplayName("Байтовый путь: случайные массивы не дают false negatives")
  void randomByteArraysHaveNoFalseNegatives() {
    Kata13_BloomFilter filter = newFilter(2_000, 0.01);
    Random random = new Random(42);
    List<byte[]> inserted = new ArrayList<>();

    for (int i = 0; i < 2_000; i++) {
      byte[] value = new byte[16];
      random.nextBytes(value);
      inserted.add(value);
      filter.put(value);
    }

    for (byte[] value : inserted) {
      assertThat(filter.mightContain(value)).isTrue();
    }
  }

  @Test
  @DisplayName("Валидация: n > 0, 0 < p < 1, адресуемый размер битового массива")
  void invalidParameters() {
    assertThatThrownBy(() -> newFilter(0, 0.01)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newFilter(-1, 0.01)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newFilter(100, 0.0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newFilter(100, 1.0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newFilter(100, -0.1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newFilter(100, 1.1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newFilter(100, Double.NaN))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newFilter(Long.MAX_VALUE, 0.01))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("null-элементы запрещены")
  void nullItemsRejected() {
    Kata13_BloomFilter filter = newFilter(10, 0.01);

    assertThatThrownBy(() -> filter.put((byte[]) null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> filter.put((String) null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> filter.mightContain((byte[]) null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> filter.mightContain((String) null))
        .isInstanceOf(NullPointerException.class);
  }
}
