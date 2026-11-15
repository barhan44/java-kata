package dev.kata.warmup.week3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week3.Warmup3_02_HappyNumberSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 3.2: Happy Number")
class Warmup3_02_HappyNumberTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup3_02_HappyNumber newSolution() {
    return USE_SOLUTION ? new Warmup3_02_HappyNumberSolution() : new Warmup3_02_HappyNumber();
  }

  @Test
  @DisplayName("Классические счастливые числа")
  void happyNumbers() {
    assertThat(newSolution().isHappy(19)).isTrue();
    assertThat(newSolution().isHappy(1)).isTrue();
    assertThat(newSolution().isHappy(7)).isTrue();
    assertThat(newSolution().isHappy(100)).isTrue();
  }

  @Test
  @DisplayName("Несчастливые числа зацикливаются")
  void unhappyNumbers() {
    assertThat(newSolution().isHappy(2)).isFalse();
    assertThat(newSolution().isHappy(3)).isFalse();
    assertThat(newSolution().isHappy(4)).isFalse();
    assertThat(newSolution().isHappy(999999999)).isFalse();
  }

  @Test
  @DisplayName("n < 1 отклоняется")
  void invalidInput() {
    assertThatThrownBy(() -> newSolution().isHappy(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().isHappy(-7))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
