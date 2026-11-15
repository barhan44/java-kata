package dev.kata.solutions.easy;

import dev.kata.easy.Easy36_FrequencyBucketCache;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Эталон Easy 36.
 *
 * <p>Частоты лежат в {@code HashMap<String, Integer>}, а монотонный номер последнего доступа — в
 * {@code HashMap<String, Long>}. {@code recordAccess} делает {@code merge(key, 1, Integer::sum)} и
 * записывает новый номер; старый номер перезаписывается. {@code leastFrequentlyUsed} сортирует
 * записи компаратором «частота ↑, номер доступа ↑»: меньший номер означает более давний доступ.
 *
 * <p>{@code limit(k)} естественно обрабатывает {@code k > size}; {@code k == 0} возвращает пустой
 * список, отрицательное — исключение. Результат {@code toList()} неизменяем.
 */
public final class Easy36_FrequencyBucketCacheSolution extends Easy36_FrequencyBucketCache {

  @Override
  public FrequencyBucketCache newFrequencyCache() {
    return new LfuTracker();
  }

  private static final class LfuTracker extends FrequencyBucketCache {

    private final Map<String, Integer> frequencies = new HashMap<>();
    private final Map<String, Long> lastAccess = new HashMap<>();
    private long sequence;

    @Override
    public void recordAccess(String key) {
      Objects.requireNonNull(key, "key");
      if (key.isBlank()) {
        throw new IllegalArgumentException("key must not be blank");
      }
      frequencies.merge(key, 1, Integer::sum);
      lastAccess.put(key, sequence++);
    }

    @Override
    public List<String> leastFrequentlyUsed(int k) {
      if (k < 0) {
        throw new IllegalArgumentException("k must be >= 0, got " + k);
      }
      if (k == 0) {
        return List.of();
      }

      return frequencies.entrySet().stream()
          .sorted(
              Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                  .thenComparingLong(entry -> lastAccess.get(entry.getKey())))
          .limit(k)
          .map(Map.Entry::getKey)
          .toList();
    }
  }
}
