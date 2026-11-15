package dev.kata.solutions.easy;

import dev.kata.easy.Easy05_MapMergeAndRollup;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Эталон Easy 05.
 *
 * <p>Копия {@code base} — это и аккумулятор, и защита исходной карты от изменений. {@code merge}
 * одним вызовом реализует «добавить или сложить»: если ключа нет, значение кладётся как есть, иначе
 * вызывается {@code Integer::sum}. После слияния {@code removeIf} убирает обнулившиеся и ушедшие в
 * минус ключи. Обёртка {@code unmodifiableMap} запрещает внешнюю мутацию результата.
 *
 * <p>Проверка ключей и значений выполняется заранее: {@code merge} с {@code null} дал бы NPE в
 * неочевидном месте, а так контракт явный.
 */
public final class Easy05_MapMergeAndRollupSolution extends Easy05_MapMergeAndRollup {

  @Override
  public Map<String, Integer> mergeBalances(Map<String, Integer> base, Map<String, Integer> delta) {
    Objects.requireNonNull(base, "base");
    Objects.requireNonNull(delta, "delta");
    requireNoNulls(base, "base");
    requireNoNulls(delta, "delta");

    Map<String, Integer> result = new HashMap<>(base);
    delta.forEach((key, value) -> result.merge(key, value, Integer::sum));
    result.entrySet().removeIf(entry -> entry.getValue() <= 0);
    return Collections.unmodifiableMap(result);
  }

  private static void requireNoNulls(Map<String, Integer> map, String name) {
    map.forEach(
        (key, value) -> {
          Objects.requireNonNull(key, name + " key");
          Objects.requireNonNull(value, name + " value");
        });
  }
}
