package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy36_FrequencyBucketCache.FrequencyBucketCache;
import dev.kata.solutions.easy.Easy36_FrequencyBucketCacheSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 36: Frequency bucket cache")
class Easy36_FrequencyBucketCacheTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy36_FrequencyBucketCache newSolution() {
    return USE_SOLUTION
        ? new Easy36_FrequencyBucketCacheSolution()
        : new Easy36_FrequencyBucketCache();
  }

  @Test
  @DisplayName("Сортирует по частоте, затем по давности доступа")
  void ordersByFrequencyThenAge() {
    FrequencyBucketCache cache = newSolution().newFrequencyCache();
    cache.recordAccess("a");
    cache.recordAccess("a");
    cache.recordAccess("b");
    cache.recordAccess("c");
    cache.recordAccess("c");

    assertThat(cache.leastFrequentlyUsed(2)).containsExactly("b", "a");
    assertThat(cache.leastFrequentlyUsed(10)).containsExactly("b", "a", "c");
  }

  @Test
  @DisplayName("Равная частота: раньше более старый доступ")
  void tieByOldestAccess() {
    FrequencyBucketCache cache = newSolution().newFrequencyCache();
    cache.recordAccess("a");
    cache.recordAccess("b");

    assertThat(cache.leastFrequentlyUsed(2)).containsExactly("a", "b");

    cache.recordAccess("a");
    cache.recordAccess("b");

    assertThat(cache.leastFrequentlyUsed(2)).containsExactly("a", "b");
  }

  @Test
  @DisplayName("k = 0 и k больше числа ключей")
  void kBoundaries() {
    FrequencyBucketCache cache = newSolution().newFrequencyCache();
    cache.recordAccess("a");

    assertThat(cache.leastFrequentlyUsed(0)).isEmpty();
    assertThat(cache.leastFrequentlyUsed(100)).containsExactly("a");
  }

  @Test
  @DisplayName("Пустой трекер")
  void emptyCache() {
    assertThat(newSolution().newFrequencyCache().leastFrequentlyUsed(5)).isEmpty();
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    FrequencyBucketCache cache = newSolution().newFrequencyCache();
    cache.recordAccess("a");

    List<String> result = cache.leastFrequentlyUsed(1);
    assertThatThrownBy(() -> result.add("b")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация ключа и k")
  void validation() {
    FrequencyBucketCache cache = newSolution().newFrequencyCache();

    assertThatThrownBy(() -> cache.recordAccess(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> cache.recordAccess(" ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> cache.leastFrequentlyUsed(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
