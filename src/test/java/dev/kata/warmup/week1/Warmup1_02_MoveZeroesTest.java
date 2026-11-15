package dev.kata.warmup.week1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week1.Warmup1_02_MoveZeroesSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 1.2: Move Zeroes")
class Warmup1_02_MoveZeroesTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup1_02_MoveZeroes newSolution() {
    return USE_SOLUTION ? new Warmup1_02_MoveZeroesSolution() : new Warmup1_02_MoveZeroes();
  }

  @Test
  @DisplayName("Классический пример")
  void movesZeroesToEnd() {
    int[] nums = {0, 1, 0, 3, 12};
    newSolution().moveZeroes(nums);
    assertThat(nums).containsExactly(1, 3, 12, 0, 0);
  }

  @Test
  @DisplayName("Без нулей и только нули")
  void edgeDistributions() {
    int[] noZeroes = {1, 2, 3};
    newSolution().moveZeroes(noZeroes);
    assertThat(noZeroes).containsExactly(1, 2, 3);

    int[] allZeroes = {0, 0, 0};
    newSolution().moveZeroes(allZeroes);
    assertThat(allZeroes).containsExactly(0, 0, 0);

    int[] single = {0};
    newSolution().moveZeroes(single);
    assertThat(single).containsExactly(0);
  }

  @Test
  @DisplayName("Порядок ненулевых сохраняется, отрицательные не теряются")
  void preservesOrder() {
    int[] nums = {0, -1, 0, -2, 3, 0};
    newSolution().moveZeroes(nums);
    assertThat(nums).containsExactly(-1, -2, 3, 0, 0, 0);
  }

  @Test
  @DisplayName("Мутация происходит в исходном массиве")
  void mutatesInPlace() {
    int[] nums = {0, 5};
    int[] reference = nums;
    newSolution().moveZeroes(nums);
    assertThat(reference).containsExactly(5, 0);
  }

  @Test
  @DisplayName("null-массив отклоняется")
  void nullArray() {
    assertThatThrownBy(() -> newSolution().moveZeroes(null))
        .isInstanceOf(NullPointerException.class);
  }
}
