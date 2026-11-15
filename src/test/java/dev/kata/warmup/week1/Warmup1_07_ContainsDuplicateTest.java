package dev.kata.warmup.week1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week1.Warmup1_07_ContainsDuplicateSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 1.7: Contains Duplicate")
class Warmup1_07_ContainsDuplicateTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup1_07_ContainsDuplicate newSolution() {
    return USE_SOLUTION
        ? new Warmup1_07_ContainsDuplicateSolution()
        : new Warmup1_07_ContainsDuplicate();
  }

  @Test
  @DisplayName("Повтор есть")
  void duplicatePresent() {
    assertThat(newSolution().containsDuplicate(new int[] {1, 2, 3, 1})).isTrue();
    assertThat(newSolution().containsDuplicate(new int[] {1, 1, 1, 3, 3, 4, 3, 2, 4, 2})).isTrue();
  }

  @Test
  @DisplayName("Все значения уникальны")
  void allUnique() {
    assertThat(newSolution().containsDuplicate(new int[] {1, 2, 3, 4})).isFalse();
    assertThat(newSolution().containsDuplicate(new int[] {7})).isFalse();
    assertThat(newSolution().containsDuplicate(new int[] {})).isFalse();
  }

  @Test
  @DisplayName("Отрицательные значения и границы int")
  void negativeAndBounds() {
    assertThat(newSolution().containsDuplicate(new int[] {-1, -1})).isTrue();
    assertThat(
            newSolution()
                .containsDuplicate(
                    new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE, Integer.MIN_VALUE}))
        .isTrue();
  }

  @Test
  @DisplayName("null-массив отклоняется")
  void nullArray() {
    assertThatThrownBy(() -> newSolution().containsDuplicate(null))
        .isInstanceOf(NullPointerException.class);
  }
}
