package dev.kata.easy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Easy 23. Граф смежности и достижимость (BFS).
 *
 * <h2>Задача</h2>
 *
 * <p>Построить граф смежности из списка направленных рёбер и найти все вершины, достижимые из
 * {@code startNode} обходом в ширину. Вершины, встречающиеся только как {@code to}, тоже должны
 * присутствовать в графе с пустым множеством исходящих рёбер.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * edges = [A→B, A→C, B→D]
 * adjacency → {A:[B,C], B:[D], C:[], D:[]}
 * reachableFrom(adjacency, "A") → [B, C, D]
 * reachableFrom(adjacency, "D") → []
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code edges.size()};
 *   <li>{@code from} и {@code to} не {@code null}/не blank.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Построение O(E log V), BFS O(V + E), память O(V + E). Эталон группирует рёбра через {@code
 * groupingBy}, добавляет вершины-стоки через {@code computeIfAbsent}, а обход делает на {@link
 * java.util.ArrayDeque}. Стартовая вершина не добавляется в результат, если в неё нет ребра (в
 * циклическом графе она может вернуться). Карты и множества неизменяемы.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy23_GraphAdjacencyReachabilitySolution}.
 */
public class Easy23_GraphAdjacencyReachability {

  /** Направленное ребро. */
  public record Edge(String from, String to) {
    public Edge {
      requireText(from, "from");
      requireText(to, "to");
    }
  }

  /** Строит граф смежности, включая вершины без исходящих рёбер. */
  public Map<String, Set<String>> buildAdjacency(List<Edge> edges) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /** Возвращает все вершины, достижимые из {@code startNode}. */
  public Set<String> reachableFrom(Map<String, Set<String>> adjacency, String startNode) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
