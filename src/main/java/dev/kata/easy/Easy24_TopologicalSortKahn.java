package dev.kata.easy;

import java.util.List;
import java.util.Objects;

/**
 * Easy 24. Топологическая сортировка (Kahn).
 *
 * <h2>Задача</h2>
 *
 * <p>Даны зависимости {@code task → dependsOn} (задача зависит от другой). Вернуть порядок
 * выполнения, в котором каждая задача идёт после всех своих зависимостей. Алгоритм Кана: очередь
 * вершин с {@code inDegree == 0}, при удалении вершины уменьшаем степени её зависимых. Если
 * обработаны не все вершины — в графе цикл, нужно бросить {@link IllegalStateException}.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * dependencies = [A→B, B→C]
 * topologicalOrder → [C, B, A]
 *
 * dependencies = [A→B, B→A]
 * topologicalOrder → IllegalStateException
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code dependencies.size()};
 *   <li>{@code task} и {@code dependsOn} не {@code null}/не blank;
 *   <li>дубликаты рёбер игнорируются.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(V + E log V), память O(V + E). Вершины и зависимые хранятся в {@link
 * java.util.TreeMap} для детерминированного порядка; очередь — {@link java.util.ArrayDeque}.
 * Дубликаты рёбер удаляются через {@code distinct()}, чтобы не завышать {@code inDegree}. Результат
 * неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy24_TopologicalSortKahnSolution}.
 */
public class Easy24_TopologicalSortKahn {

  /** Зависимость: {@code task} требует {@code dependsOn}. */
  public record Dependency(String task, String dependsOn) {
    public Dependency {
      requireText(task, "task");
      requireText(dependsOn, "dependsOn");
    }
  }

  /** Возвращает топологический порядок или бросает исключение при цикле. */
  public List<String> topologicalOrder(List<Dependency> dependencies) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
