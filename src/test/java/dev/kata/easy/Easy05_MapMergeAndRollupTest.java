package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy05_MapMergeAndRollupSolution;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 05: Map merge and rollup")
class Easy05_MapMergeAndRollupTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy05_MapMergeAndRollup newSolution() {
    return USE_SOLUTION ? new Easy05_MapMergeAndRollupSolution() : new Easy05_MapMergeAndRollup();
  }

  @Test
  @DisplayName("Суммирует совпадающие ключи и убирает неположительные итоги")
  void mergesAndRollsUp() {
    Map<String, Integer> base = Map.of("a", 10, "b", 5, "c", 3);
    Map<String, Integer> delta = Map.of("a", -10, "b", 2, "d", 7, "e", -1);

    Map<String, Integer> result = newSolution().mergeBalances(base, delta);

    assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("b", 7, "c", 3, "d", 7));
  }

  @Test
  @DisplayName("Исходные карты не изменяются")
  void doesNotMutateInputs() {
    Map<String, Integer> base = new HashMap<>(Map.of("a", 5));
    Map<String, Integer> delta = new HashMap<>(Map.of("a", -5, "b", 1));

    newSolution().mergeBalances(base, delta);

    assertThat(base).containsExactlyEntriesOf(Map.of("a", 5));
    assertThat(delta).containsExactlyInAnyOrderEntriesOf(Map.of("a", -5, "b", 1));
  }

  @Test
  @DisplayName("Пустые карты и только отрицательные значения")
  void emptyAndNegative() {
    assertThat(newSolution().mergeBalances(Map.of(), Map.of())).isEmpty();
    assertThat(newSolution().mergeBalances(Map.of("a", 1), Map.of())).containsEntry("a", 1);
    assertThat(newSolution().mergeBalances(Map.of(), Map.of("a", 2))).containsEntry("a", 2);
    assertThat(newSolution().mergeBalances(Map.of("a", 1), Map.of("a", -1))).isEmpty();
    assertThat(newSolution().mergeBalances(Map.of("a", 1), Map.of("a", -2))).isEmpty();
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    Map<String, Integer> result = newSolution().mergeBalances(Map.of("a", 1), Map.of());
    assertThatThrownBy(() -> result.put("b", 2)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("null-карты и null-элементы отклоняются")
  void validation() {
    assertThatThrownBy(() -> newSolution().mergeBalances(null, Map.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().mergeBalances(Map.of(), null))
        .isInstanceOf(NullPointerException.class);

    Map<String, Integer> nullValue = new HashMap<>();
    nullValue.put("a", null);
    assertThatThrownBy(() -> newSolution().mergeBalances(nullValue, Map.of()))
        .isInstanceOf(NullPointerException.class);

    Map<String, Integer> nullKey = new HashMap<>();
    nullKey.put(null, 1);
    assertThatThrownBy(() -> newSolution().mergeBalances(Map.of(), nullKey))
        .isInstanceOf(NullPointerException.class);
  }
}
