package dev.kata.solutions.easy;

import dev.kata.easy.Easy23_GraphAdjacencyReachability;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Эталон Easy 23.
 *
 * <p>{@code groupingBy(from, TreeMap, mapping(to, LinkedHashSet))} собирает исходящие рёбра и
 * упорядочивает вершины. Отдельный проход по рёбрам добавляет {@code to}-вершины, у которых нет
 * исходящих рёбер, через {@code computeIfAbsent} — иначе BFS не увидел бы стоки. Все множества
 * копируются в unmodifiable-обёртки.
 *
 * <p>BFS использует {@link ArrayDeque} как FIFO: {@code removeFirst} достаёт текущую вершину,
 * {@code addLast} добавляет соседей. {@code visited} одновременно предотвращает повторный обход и
 * задаёт порядок результата. Стартовая вершина попадает в результат только если до неё реально есть
 * путь (например, в цикле).
 */
public final class Easy23_GraphAdjacencyReachabilitySolution
    extends Easy23_GraphAdjacencyReachability {

  @Override
  public Map<String, Set<String>> buildAdjacency(List<Edge> edges) {
    Objects.requireNonNull(edges, "edges");

    Map<String, Set<String>> grouped =
        edges.stream()
            .collect(
                Collectors.groupingBy(
                    Edge::from,
                    TreeMap::new,
                    Collectors.mapping(Edge::to, Collectors.toCollection(LinkedHashSet::new))));
    edges.forEach(edge -> grouped.computeIfAbsent(edge.to(), ignored -> new LinkedHashSet<>()));

    Map<String, Set<String>> adjacency = new TreeMap<>();
    grouped.forEach(
        (from, targets) ->
            adjacency.put(from, Collections.unmodifiableSet(new LinkedHashSet<>(targets))));
    return Collections.unmodifiableMap(adjacency);
  }

  @Override
  public Set<String> reachableFrom(Map<String, Set<String>> adjacency, String startNode) {
    Objects.requireNonNull(adjacency, "adjacency");
    Objects.requireNonNull(startNode, "startNode");

    Set<String> visited = new LinkedHashSet<>();
    ArrayDeque<String> queue = new ArrayDeque<>();
    queue.addLast(startNode);

    while (!queue.isEmpty()) {
      String node = queue.removeFirst();
      for (String next : adjacency.getOrDefault(node, Set.of())) {
        if (visited.add(next)) {
          queue.addLast(next);
        }
      }
    }
    return Collections.unmodifiableSet(visited);
  }
}
