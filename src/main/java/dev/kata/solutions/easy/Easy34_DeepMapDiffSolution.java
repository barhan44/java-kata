package dev.kata.solutions.easy;

import dev.kata.easy.Easy34_DeepMapDiff;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Эталон Easy 34.
 *
 * <p>{@link #flatten} рекурсивно обходит мапу: если значение — снова {@code Map}, путь удлиняется
 * через точку, иначе пара попадает в плоскую карту. Пустые вложенные мапы не дают листьев и потому
 * не порождают различий сами по себе.
 *
 * <p>Различия собираются по объединению путей в {@link TreeSet} — порядок детерминирован. Для
 * каждой стороны отдельно проверяется наличие ключа: это позволяет отличить отсутствие листа от
 * листа со значением {@code null}. Итоговая карта — unmodifiable {@link TreeMap}.
 */
public final class Easy34_DeepMapDiffSolution extends Easy34_DeepMapDiff {

  private static final String MISSING = "<missing>";

  @Override
  public Map<String, String> diffNestedMaps(Map<String, Object> left, Map<String, Object> right) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");

    Map<String, Object> leftLeaves = new TreeMap<>();
    Map<String, Object> rightLeaves = new TreeMap<>();
    flatten(left, "", leftLeaves);
    flatten(right, "", rightLeaves);

    TreeSet<String> paths = new TreeSet<>(leftLeaves.keySet());
    paths.addAll(rightLeaves.keySet());

    Map<String, String> differences = new TreeMap<>();
    for (String path : paths) {
      boolean inLeft = leftLeaves.containsKey(path);
      boolean inRight = rightLeaves.containsKey(path);
      Object leftValue = leftLeaves.get(path);
      Object rightValue = rightLeaves.get(path);
      if (inLeft && inRight && Objects.equals(leftValue, rightValue)) {
        continue;
      }
      differences.put(
          path, path + ": " + display(leftValue, inLeft) + " -> " + display(rightValue, inRight));
    }
    return Collections.unmodifiableMap(differences);
  }

  private static void flatten(Map<?, ?> map, String prefix, Map<String, Object> leaves) {
    map.forEach(
        (key, value) -> {
          String path = prefix.isEmpty() ? String.valueOf(key) : prefix + "." + key;
          if (value instanceof Map<?, ?> nested) {
            flatten(nested, path, leaves);
          } else {
            leaves.put(path, value);
          }
        });
  }

  private static String display(Object value, boolean present) {
    return present ? String.valueOf(value) : MISSING;
  }
}
