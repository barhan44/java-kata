package dev.kata.warmup.week2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week2.Warmup2_04_SingleNumberSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 2.4: Single Number")
class Warmup2_04_SingleNumberTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup2_04_SingleNumber newSolution() {
    return USE_SOLUTION ? new Warmup2_04_SingleNumberSolution() : new Warmup2_04_SingleNumber();
  }

  @Test
  @DisplayName("Классические примеры")
  void examples() {
    assertThat(newSolution().singleNumber(new int[] {2, 2, 1})).isEqualTo(1);
    assertThat(newSolution().singleNumber(new int[] {4, 1, 2, 1, 2})).isEqualTo(4);
  }

  @Test
  @DisplayName("Один элемент и отрицательные значения")
  void singleAndNegatives() {
    assertThat(newSolution().singleNumber(new int[] {7})).isEqualTo(7);
    assertThat(newSolution().singleNumber(new int[] {-3, 5, 5})).isEqualTo(-3);
    assertThat(newSolution().singleNumber(new int[] {-1, -1, -2})).isEqualTo(-2);
  }

  @Test
  @DisplayName("Границы int")
  void integerBounds() {
    assertThat(
            newSolution()
                .singleNumber(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE}))
        .isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  @DisplayName("Пустой массив и null отклоняются")
  void invalidInput() {
    assertThatThrownBy(() -> newSolution().singleNumber(new int[] {}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().singleNumber(null))
        .isInstanceOf(NullPointerException.class);
  }
}
