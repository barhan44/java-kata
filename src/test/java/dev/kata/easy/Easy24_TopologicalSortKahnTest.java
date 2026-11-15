package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy24_TopologicalSortKahn.Dependency;
import dev.kata.solutions.easy.Easy24_TopologicalSortKahnSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 24: Topological sort (Kahn)")
class Easy24_TopologicalSortKahnTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy24_TopologicalSortKahn newSolution() {
    return USE_SOLUTION
        ? new Easy24_TopologicalSortKahnSolution()
        : new Easy24_TopologicalSortKahn();
  }

  @Test
  @DisplayName("Цепочка зависимостей")
  void chain() {
    List<Dependency> dependencies = List.of(new Dependency("A", "B"), new Dependency("B", "C"));

    assertThat(newSolution().topologicalOrder(dependencies)).containsExactly("C", "B", "A");
  }

  @Test
  @DisplayName("Ветвление: зависимость после всех предков")
  void branching() {
    List<Dependency> dependencies =
        List.of(
            new Dependency("B", "A"),
            new Dependency("C", "A"),
            new Dependency("D", "B"),
            new Dependency("D", "C"));

    assertThat(newSolution().topologicalOrder(dependencies)).containsExactly("A", "B", "C", "D");
  }

  @Test
  @DisplayName("Независимые корни и дубликаты рёбер")
  void independentRootsAndDuplicates() {
    assertThat(
            newSolution()
                .topologicalOrder(
                    List.of(
                        new Dependency("B", "A"),
                        new Dependency("B", "A"),
                        new Dependency("C", "A"))))
        .containsExactly("A", "B", "C");
  }

  @Test
  @DisplayName("Цикл и самозависимость отклоняются")
  void cycles() {
    assertThatThrownBy(
            () ->
                newSolution()
                    .topologicalOrder(List.of(new Dependency("A", "B"), new Dependency("B", "A"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cycle");

    assertThatThrownBy(() -> newSolution().topologicalOrder(List.of(new Dependency("A", "A"))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Пустой список")
  void emptyList() {
    assertThat(newSolution().topologicalOrder(List.of())).isEmpty();
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<String> result = newSolution().topologicalOrder(List.of(new Dependency("A", "B")));
    assertThatThrownBy(() -> result.add("C")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация Dependency и null-списка")
  void validation() {
    assertThatThrownBy(() -> new Dependency(null, "B")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Dependency(" ", "B")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Dependency("A", " ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().topologicalOrder(null))
        .isInstanceOf(NullPointerException.class);
  }
}
