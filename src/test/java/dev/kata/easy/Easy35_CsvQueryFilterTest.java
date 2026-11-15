package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy35_CsvQueryFilterSolution;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 35: CSV filter and project")
class Easy35_CsvQueryFilterTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy35_CsvQueryFilter newSolution() {
    return USE_SOLUTION ? new Easy35_CsvQueryFilterSolution() : new Easy35_CsvQueryFilter();
  }

  private static List<Map<String, String>> rows() {
    return List.of(
        Map.of("name", "Alice", "dept", "Eng", "salary", "120"),
        Map.of("name", "Bob", "dept", "Eng", "salary", "80"),
        Map.of("name", "Carol", "dept", "Sales", "salary", "90"));
  }

  @Test
  @DisplayName("Фильтрует по всем предикатам и проецирует в заданном порядке")
  void filtersAndProjects() {
    Map<String, Predicate<String>> filters =
        Map.of("dept", "Eng"::equals, "salary", value -> Integer.parseInt(value) > 100);

    List<Map<String, String>> result =
        newSolution().filterAndProject(rows(), filters, List.of("name", "salary"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).keySet()).containsExactly("name", "salary");
    assertThat(result.get(0)).containsEntry("name", "Alice").containsEntry("salary", "120");
  }

  @Test
  @DisplayName("Пустые фильтры пропускают все строки")
  void noFilters() {
    List<Map<String, String>> result =
        newSolution().filterAndProject(rows(), Map.of(), List.of("name"));

    assertThat(result).extracting(row -> row.get("name")).containsExactly("Alice", "Bob", "Carol");
  }

  @Test
  @DisplayName("Пустая проекция даёт пустые карты на каждую подходящую строку")
  void emptyProjection() {
    List<Map<String, String>> result =
        newSolution().filterAndProject(rows(), Map.of("dept", "Eng"::equals), List.of());

    assertThat(result).hasSize(2).allSatisfy(row -> assertThat(row).isEmpty());
  }

  @Test
  @DisplayName("Нет совпадений и пустой вход")
  void noMatchesAndEmptyInput() {
    assertThat(
            newSolution().filterAndProject(rows(), Map.of("dept", "HR"::equals), List.of("name")))
        .isEmpty();
    assertThat(newSolution().filterAndProject(List.of(), Map.of(), List.of("name"))).isEmpty();
  }

  @Test
  @DisplayName("Отсутствующая колонка: фильтр получает null, проекция содержит null")
  void missingColumn() {
    Map<String, Predicate<String>> filters = Map.of("missing", value -> value == null);
    List<Map<String, String>> result =
        newSolution().filterAndProject(rows(), filters, List.of("name", "missing"));

    assertThat(result).hasSize(3);
    assertThat(result.get(0)).containsKey("missing");
    assertThat(result.get(0).get("missing")).isNull();
  }

  @Test
  @DisplayName("Результат и строки неизменяемы, вход не меняется")
  void immutability() {
    List<Map<String, String>> input = rows();
    List<Map<String, String>> result =
        newSolution().filterAndProject(input, Map.of(), List.of("name"));

    assertThat(input).hasSize(3);
    assertThatThrownBy(() -> result.add(Map.of()))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.get(0).put("x", "y"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("null-аргументы и null-предикат отклоняются")
  void validation() {
    assertThatThrownBy(() -> newSolution().filterAndProject(null, Map.of(), List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().filterAndProject(rows(), null, List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().filterAndProject(rows(), Map.of(), null))
        .isInstanceOf(NullPointerException.class);

    Map<String, Predicate<String>> nullPredicate = new HashMap<>();
    nullPredicate.put("name", null);
    assertThatThrownBy(() -> newSolution().filterAndProject(rows(), nullPredicate, List.of("name")))
        .isInstanceOf(NullPointerException.class);
  }
}
