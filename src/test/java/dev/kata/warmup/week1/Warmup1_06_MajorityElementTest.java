package dev.kata.warmup.week1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week1.Warmup1_06_MajorityElementSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 1.6: Majority Element")
class Warmup1_06_MajorityElementTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup1_06_MajorityElement newSolution() {
    return USE_SOLUTION
        ? new Warmup1_06_MajorityElementSolution()
        : new Warmup1_06_MajorityElement();
  }

  @Test
  @DisplayName("Классические примеры LeetCode")
  void examples() {
    assertThat(newSolution().majorityElement(new int[] {3, 2, 3})).isEqualTo(3);
    assertThat(newSolution().majorityElement(new int[] {2, 2, 1, 1, 1, 2, 2})).isEqualTo(2);
  }

  @Test
  @DisplayName("Один элемент и все одинаковые")
  void singleValue() {
    assertThat(newSolution().majorityElement(new int[] {42})).isEqualTo(42);
    assertThat(newSolution().majorityElement(new int[] {-5, -5, -5})).isEqualTo(-5);
  }

  @Test
  @DisplayName("Элемент большинства в конце не теряется")
  void majorityAtEnd() {
    assertThat(newSolution().majorityElement(new int[] {1, 1, 2, 2, 2})).isEqualTo(2);
    assertThat(newSolution().majorityElement(new int[] {1, 2, 1, 3, 1})).isEqualTo(1);
  }

  @Test
  @DisplayName("Пустой массив и отсутствие большинства отклоняются")
  void invalidInput() {
    assertThatThrownBy(() -> newSolution().majorityElement(new int[] {}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().majorityElement(new int[] {1, 2, 3}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().majorityElement(null))
        .isInstanceOf(NullPointerException.class);
  }
}
