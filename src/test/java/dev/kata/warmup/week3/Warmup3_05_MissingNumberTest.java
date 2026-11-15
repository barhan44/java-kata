package dev.kata.warmup.week3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week3.Warmup3_05_MissingNumberSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 3.5: Missing Number")
class Warmup3_05_MissingNumberTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup3_05_MissingNumber newSolution() {
    return USE_SOLUTION ? new Warmup3_05_MissingNumberSolution() : new Warmup3_05_MissingNumber();
  }

  @Test
  @DisplayName("Классические примеры")
  void examples() {
    assertThat(newSolution().missingNumber(new int[] {3, 0, 1})).isEqualTo(2);
    assertThat(newSolution().missingNumber(new int[] {0, 1})).isEqualTo(2);
    assertThat(newSolution().missingNumber(new int[] {9, 6, 4, 2, 3, 5, 7, 0, 1})).isEqualTo(8);
  }

  @Test
  @DisplayName("Пропущен первый или последний элемент")
  void boundaries() {
    assertThat(newSolution().missingNumber(new int[] {1})).isZero();
    assertThat(newSolution().missingNumber(new int[] {0})).isEqualTo(1);
    assertThat(newSolution().missingNumber(new int[] {0, 2, 3})).isEqualTo(1);
    assertThat(newSolution().missingNumber(new int[] {0, 1, 2})).isEqualTo(3);
  }

  @Test
  @DisplayName("Пустой массив — отсутствует 0")
  void emptyArray() {
    assertThat(newSolution().missingNumber(new int[] {})).isZero();
  }

  @Test
  @DisplayName("null-массив отклоняется")
  void nullArray() {
    assertThatThrownBy(() -> newSolution().missingNumber(null))
        .isInstanceOf(NullPointerException.class);
  }
}
