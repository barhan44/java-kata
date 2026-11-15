package dev.kata.warmup.week1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week1.Warmup1_01_TwoSumSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 1.1: Two Sum")
class Warmup1_01_TwoSumTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup1_01_TwoSum newSolution() {
    return USE_SOLUTION ? new Warmup1_01_TwoSumSolution() : new Warmup1_01_TwoSum();
  }

  @Test
  @DisplayName("Классический пример и пример с дубликатом")
  void examples() {
    assertThat(newSolution().twoSum(new int[] {2, 7, 11, 15}, 9)).containsExactly(0, 1);
    assertThat(newSolution().twoSum(new int[] {3, 2, 4}, 6)).containsExactly(1, 2);
    assertThat(newSolution().twoSum(new int[] {3, 3}, 6)).containsExactly(0, 1);
  }

  @Test
  @DisplayName("Отрицательные числа и ноль")
  void negativesAndZero() {
    assertThat(newSolution().twoSum(new int[] {-3, 4, 3, 90}, 0)).containsExactly(0, 2);
    assertThat(newSolution().twoSum(new int[] {0, 4, 3, 0}, 0)).containsExactly(0, 3);
  }

  @Test
  @DisplayName("Границы int не переполняют дополнение")
  void integerBounds() {
    assertThat(newSolution().twoSum(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE}, -1))
        .containsExactly(0, 1);
  }

  @Test
  @DisplayName("Отсутствие решения и null-массив")
  void invalidInput() {
    assertThatThrownBy(() -> newSolution().twoSum(new int[] {1, 2}, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().twoSum(null, 1))
        .isInstanceOf(NullPointerException.class);
  }
}
