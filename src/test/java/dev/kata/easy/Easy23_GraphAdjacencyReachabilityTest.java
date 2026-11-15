package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy23_GraphAdjacencyReachability.Edge;
import dev.kata.solutions.easy.Easy23_GraphAdjacencyReachabilitySolution;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 23: Graph adjacency and reachability")
class Easy23_GraphAdjacencyReachabilityTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy23_GraphAdjacencyReachability newSolution() {
    return USE_SOLUTION
        ? new Easy23_GraphAdjacencyReachabilitySolution()
        : new Easy23_GraphAdjacencyReachability();
  }

  @Test
  @DisplayName("Строит смежность, включая вершины-стоки")
  void buildsAdjacency() {
    Map<String, Set<String>> adjacency =
        newSolution()
            .buildAdjacency(List.of(new Edge("A", "B"), new Edge("A", "C"), new Edge("B", "D")));

    assertThat(adjacency.keySet()).containsExactly("A", "B", "C", "D");
    assertThat(adjacency.get("A")).containsExactly("B", "C");
    assertThat(adjacency.get("B")).containsExactly("D");
    assertThat(adjacency.get("C")).isEmpty();
    assertThat(adjacency.get("D")).isEmpty();
  }

  @Test
  @DisplayName("BFS возвращает достижимые вершины в порядке обхода")
  void bfsReachability() {
    Map<String, Set<String>> adjacency =
        newSolution()
            .buildAdjacency(List.of(new Edge("A", "B"), new Edge("A", "C"), new Edge("B", "D")));

    assertThat(newSolution().reachableFrom(adjacency, "A")).containsExactly("B", "C", "D");
    assertThat(newSolution().reachableFrom(adjacency, "B")).containsExactly("D");
    assertThat(newSolution().reachableFrom(adjacency, "D")).isEmpty();
    assertThat(newSolution().reachableFrom(adjacency, "unknown")).isEmpty();
  }

  @Test
  @DisplayName("Цикл: стартовая вершина достижима через ребро")
  void cycleCanReachStart() {
    Map<String, Set<String>> adjacency =
        newSolution().buildAdjacency(List.of(new Edge("A", "B"), new Edge("B", "A")));

    assertThat(newSolution().reachableFrom(adjacency, "A")).containsExactly("B", "A");
  }

  @Test
  @DisplayName("Пустой граф")
  void emptyGraph() {
    assertThat(newSolution().buildAdjacency(List.of())).isEmpty();
    assertThat(newSolution().reachableFrom(Map.of(), "A")).isEmpty();
  }

  @Test
  @DisplayName("Карты и множества неизменяемы")
  void immutableGraph() {
    Map<String, Set<String>> adjacency = newSolution().buildAdjacency(List.of(new Edge("A", "B")));

    assertThatThrownBy(() -> adjacency.put("C", Set.of()))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> adjacency.get("A").add("C"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация Edge и null-аргументов")
  void validation() {
    assertThatThrownBy(() -> new Edge(null, "B")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Edge(" ", "B")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Edge("A", " ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().buildAdjacency(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().reachableFrom(null, "A"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().reachableFrom(Map.of(), null))
        .isInstanceOf(NullPointerException.class);
  }
}
