package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy33_RecursiveMenuFlattener.MenuItem;
import dev.kata.solutions.easy.Easy33_RecursiveMenuFlattenerSolution;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 33: Recursive menu flattener")
class Easy33_RecursiveMenuFlattenerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy33_RecursiveMenuFlattener newSolution() {
    return USE_SOLUTION
        ? new Easy33_RecursiveMenuFlattenerSolution()
        : new Easy33_RecursiveMenuFlattener();
  }

  @Test
  @DisplayName("Строит пути depth-first pre-order")
  void flattensNestedMenu() {
    MenuItem laptops = new MenuItem("laptops", "Ноутбуки", List.of());
    MenuItem catalog = new MenuItem("catalog", "Каталог", List.of(laptops));
    MenuItem home = new MenuItem("home", "Главная", List.of(catalog));

    Map<String, String> result = newSolution().flattenMenuPaths(List.of(home));

    assertThat(result.keySet()).containsExactly("home", "catalog", "laptops");
    assertThat(result)
        .containsEntry("home", "Главная")
        .containsEntry("catalog", "Главная / Каталог")
        .containsEntry("laptops", "Главная / Каталог / Ноутбуки");
  }

  @Test
  @DisplayName("Несколько корней сохраняют порядок")
  void multipleRoots() {
    MenuItem second = new MenuItem("b", "B", List.of());
    MenuItem first = new MenuItem("a", "A", List.of());

    assertThat(newSolution().flattenMenuPaths(List.of(second, first)).keySet())
        .containsExactly("b", "a");
  }

  @Test
  @DisplayName("Пустой список и пункты без детей")
  void emptyCases() {
    assertThat(newSolution().flattenMenuPaths(List.of())).isEmpty();
    assertThat(newSolution().flattenMenuPaths(List.of(new MenuItem("a", "A", List.of()))))
        .containsEntry("a", "A");
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    Map<String, String> result =
        newSolution().flattenMenuPaths(List.of(new MenuItem("a", "A", List.of())));
    assertThatThrownBy(() -> result.put("b", "B"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация MenuItem и null-списка")
  void validation() {
    assertThatThrownBy(() -> new MenuItem(null, "A", List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new MenuItem(" ", "A", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new MenuItem("a", " ", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new MenuItem("a", "A", null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().flattenMenuPaths(null))
        .isInstanceOf(NullPointerException.class);
  }
}
