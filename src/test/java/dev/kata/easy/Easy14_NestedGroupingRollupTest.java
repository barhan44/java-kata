package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy14_NestedGroupingRollup.Sale;
import dev.kata.solutions.easy.Easy14_NestedGroupingRollupSolution;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 14: Nested grouping and rollup")
class Easy14_NestedGroupingRollupTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy14_NestedGroupingRollup newSolution() {
    return USE_SOLUTION
        ? new Easy14_NestedGroupingRollupSolution()
        : new Easy14_NestedGroupingRollup();
  }

  @Test
  @DisplayName("Суммирует доход по продукту и выбирает максимум")
  void sumsRevenuePerProduct() {
    List<Sale> sales =
        List.of(
            new Sale("North", "Toys", "Lego", 100),
            new Sale("North", "Toys", "Barbie", 150),
            new Sale("North", "Toys", "Lego", 100));

    assertThat(newSolution().bestProductByRegionAndCategory(sales))
        .containsEntry("North", Map.of("Toys", "Lego"));
  }

  @Test
  @DisplayName("При равном доходе выбирается меньший product")
  void tieBreakAlphabetical() {
    List<Sale> sales =
        List.of(new Sale("South", "Books", "Beta", 100), new Sale("South", "Books", "Alpha", 100));

    assertThat(newSolution().bestProductByRegionAndCategory(sales))
        .containsEntry("South", Map.of("Books", "Alpha"));
  }

  @Test
  @DisplayName("Несколько регионов и категорий упорядочены")
  void nestedMapStructure() {
    List<Sale> sales =
        List.of(
            new Sale("West", "Books", "A", 10),
            new Sale("East", "Toys", "B", 20),
            new Sale("East", "Books", "C", 30));

    Map<String, Map<String, String>> result = newSolution().bestProductByRegionAndCategory(sales);

    assertThat(result.keySet()).containsExactly("East", "West");
    assertThat(result.get("East").keySet()).containsExactly("Books", "Toys");
    assertThat(result).containsEntry("East", Map.of("Books", "C", "Toys", "B"));
    assertThat(result).containsEntry("West", Map.of("Books", "A"));
  }

  @Test
  @DisplayName("Пустой список")
  void emptyList() {
    assertThat(newSolution().bestProductByRegionAndCategory(List.of())).isEmpty();
  }

  @Test
  @DisplayName("Карты неизменяемы")
  void immutableMaps() {
    Map<String, Map<String, String>> result =
        newSolution()
            .bestProductByRegionAndCategory(List.of(new Sale("North", "Toys", "Lego", 100)));

    assertThatThrownBy(() -> result.put("South", Map.of()))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.get("North").put("Books", "X"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация Sale и null-списка")
  void validation() {
    assertThatThrownBy(() -> new Sale(null, "Toys", "Lego", 1))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Sale(" ", "Toys", "Lego", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Sale("North", " ", "Lego", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Sale("North", "Toys", " ", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().bestProductByRegionAndCategory(null))
        .isInstanceOf(NullPointerException.class);
  }
}
