package dev.kata.solutions.easy;

import dev.kata.easy.Easy24_TopologicalSortKahn;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Эталон Easy 24.
 *
 * <p>Сначала {@code distinct()} убирает повторные рёбра: иначе {@code inDegree} задачи вырос бы
 * дважды, а зависимый был бы добавлен в множество один раз, и алгоритм ложно обнаружил бы цикл.
 * Затем строятся {@code inDegree} и {@code dependsOn → tasks}. {@link TreeMap} и {@link TreeSet}
 * дают детерминированный порядок: среди готовых задач всегда берётся лексикографически первая.
 *
 * <p>Очередь Кана — {@link ArrayDeque}. Если после обработки остались вершины с ненулевой степенью,
 * значит, они заперты в цикле: бросаем {@link IllegalStateException}. Результат копируется через
 * {@code List.copyOf}.
 */
public final class Easy24_TopologicalSortKahnSolution extends Easy24_TopologicalSortKahn {

  @Override
  public List<String> topologicalOrder(List<Dependency> dependencies) {
    Objects.requireNonNull(dependencies, "dependencies");

    List<Dependency> unique = dependencies.stream().distinct().toList();
    Map<String, Integer> inDegree = new TreeMap<>();
    Map<String, Set<String>> dependents = new TreeMap<>();

    for (Dependency dependency : unique) {
      inDegree.putIfAbsent(dependency.task(), 0);
      inDegree.putIfAbsent(dependency.dependsOn(), 0);
      dependents
          .computeIfAbsent(dependency.dependsOn(), ignored -> new TreeSet<>())
          .add(dependency.task());
      inDegree.merge(dependency.task(), 1, Integer::sum);
    }

    ArrayDeque<String> ready =
        inDegree.entrySet().stream()
            .filter(entry -> entry.getValue() == 0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(ArrayDeque::new));

    List<String> order = new ArrayList<>(inDegree.size());
    while (!ready.isEmpty()) {
      String task = ready.removeFirst();
      order.add(task);
      for (String dependent : dependents.getOrDefault(task, Set.of())) {
        int remaining = inDegree.get(dependent) - 1;
        inDegree.put(dependent, remaining);
        if (remaining == 0) {
          ready.addLast(dependent);
        }
      }
    }

    if (order.size() != inDegree.size()) {
      throw new IllegalStateException("Cycle detected in dependencies");
    }
    return List.copyOf(order);
  }
}
