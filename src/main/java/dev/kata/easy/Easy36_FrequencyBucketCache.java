package dev.kata.easy;

import java.util.List;

/**
 * Easy 36. Упрощённый LFU-трекер.
 *
 * <h2>Задача</h2>
 *
 * <p>{@link FrequencyBucketCache} считает обращения к ключам. {@code recordAccess(key)} увеличивает
 * частоту и обновляет время доступа. {@code leastFrequentlyUsed(k)} возвращает до {@code k} ключей
 * с наименьшей частотой; при равной частоте раньше идёт ключ, к которому дольше не обращались.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * recordAccess("a"); recordAccess("a"); recordAccess("b"); recordAccess("c"); recordAccess("c");
 * leastFrequentlyUsed(2) → ["b", "a"]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>{@code key} не {@code null}/не blank; {@code k ≥ 0}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>{@code recordAccess} — O(1) в среднем: частота хранится в {@code HashMap}, а монотонный номер
 * доступа — в втором map. {@code leastFrequentlyUsed} — O(u log u + k), где u — число ключей:
 * сортировка по частоте и времени доступа. Результат неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy36_FrequencyBucketCacheSolution}.
 */
public class Easy36_FrequencyBucketCache {

  /** Трекер частот и давности обращений. */
  public static class FrequencyBucketCache {

    protected FrequencyBucketCache() {}

    /** Регистрирует обращение к ключу. */
    public void recordAccess(String key) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Возвращает до {@code k} наименее частых ключей. */
    public List<String> leastFrequentlyUsed(int k) {
      throw new UnsupportedOperationException("Not implemented yet");
    }
  }

  /** Фабрика трекера: тесты выбирают заготовку или эталон. */
  public FrequencyBucketCache newFrequencyCache() {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
