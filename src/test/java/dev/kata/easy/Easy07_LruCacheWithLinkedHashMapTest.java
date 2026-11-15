package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy07_LruCacheWithLinkedHashMap.LruCache;
import dev.kata.solutions.easy.Easy07_LruCacheWithLinkedHashMapSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 07: LRU cache on LinkedHashMap")
class Easy07_LruCacheWithLinkedHashMapTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy07_LruCacheWithLinkedHashMap newSolution() {
    return USE_SOLUTION
        ? new Easy07_LruCacheWithLinkedHashMapSolution()
        : new Easy07_LruCacheWithLinkedHashMap();
  }

  @Test
  @DisplayName("Вытесняется самая давно использованная запись")
  void evictsLeastRecentlyUsed() {
    LruCache<String, Integer> cache = newSolution().newLruCache(2);

    cache.put("a", 1);
    cache.put("b", 2);
    assertThat(cache.keysInAccessOrder()).containsExactly("a", "b");

    cache.put("c", 3);
    assertThat(cache.size()).isEqualTo(2);
    assertThat(cache.keysInAccessOrder()).containsExactly("b", "c");
    assertThat(cache.get("a")).isEmpty();
  }

  @Test
  @DisplayName("get обновляет порядок доступа")
  void getRefreshesAccessOrder() {
    LruCache<String, Integer> cache = newSolution().newLruCache(2);
    cache.put("a", 1);
    cache.put("b", 2);

    assertThat(cache.get("a")).contains(1);
    assertThat(cache.keysInAccessOrder()).containsExactly("b", "a");

    cache.put("c", 3);
    assertThat(cache.keysInAccessOrder()).containsExactly("a", "c");
    assertThat(cache.get("b")).isEmpty();
  }

  @Test
  @DisplayName("Повторный put обновляет значение и порядок")
  void putExistingKeyUpdatesAndRefreshes() {
    LruCache<String, Integer> cache = newSolution().newLruCache(2);
    cache.put("a", 1);
    cache.put("b", 2);

    cache.put("a", 10);

    assertThat(cache.get("a")).contains(10);
    assertThat(cache.keysInAccessOrder()).containsExactly("b", "a");
    assertThat(cache.size()).isEqualTo(2);
  }

  @Test
  @DisplayName("Ёмкость 1 и промах не меняют порядок")
  void capacityOneAndMiss() {
    LruCache<String, Integer> cache = newSolution().newLruCache(1);
    cache.put("a", 1);
    assertThat(cache.get("missing")).isEmpty();
    assertThat(cache.keysInAccessOrder()).containsExactly("a");

    cache.put("b", 2);
    assertThat(cache.keysInAccessOrder()).containsExactly("b");
    assertThat(cache.get("a")).isEmpty();
  }

  @Test
  @DisplayName("Пустой кэш")
  void emptyCache() {
    LruCache<String, Integer> cache = newSolution().newLruCache(3);
    assertThat(cache.size()).isZero();
    assertThat(cache.keysInAccessOrder()).isEmpty();
    assertThat(cache.get("a")).isEmpty();
  }

  @Test
  @DisplayName("Список ключей неизменяем")
  void keysAreImmutable() {
    LruCache<String, Integer> cache = newSolution().newLruCache(2);
    cache.put("a", 1);

    assertThatThrownBy(() -> cache.keysInAccessOrder().add("b"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Некорректная ёмкость и null-аргументы")
  void validation() {
    assertThatThrownBy(() -> newSolution().newLruCache(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().newLruCache(-1))
        .isInstanceOf(IllegalArgumentException.class);

    LruCache<String, Integer> cache = newSolution().newLruCache(2);
    assertThatThrownBy(() -> cache.get(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> cache.put(null, 1)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> cache.put("a", null)).isInstanceOf(NullPointerException.class);
  }
}
