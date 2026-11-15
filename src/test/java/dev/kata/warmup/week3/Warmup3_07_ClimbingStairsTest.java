package dev.kata.warmup.week3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week3.Warmup3_07_ClimbingStairsSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 3.7: Climbing Stairs")
class Warmup3_07_ClimbingStairsTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup3_07_ClimbingStairs newSolution() {
    return USE_SOLUTION ? new Warmup3_07_ClimbingStairsSolution() : new Warmup3_07_ClimbingStairs();
  }

  @Test
  @DisplayName("Малые значения")
  void smallValues() {
    assertThat(newSolution().climbStairs(1)).isEqualTo(1);
    assertThat(newSolution().climbStairs(2)).isEqualTo(2);
    assertThat(newSolution().climbStairs(3)).isEqualTo(3);
    assertThat(newSolution().climbStairs(5)).isEqualTo(8);
  }

  @Test
  @DisplayName("Числа Фибоначчи для n = 10 и n = 45")
  void largerValues() {
    assertThat(newSolution().climbStairs(10)).isEqualTo(89);
    assertThat(newSolution().climbStairs(45)).isEqualTo(1836311903);
  }

  @Test
  @DisplayName("n вне [1, 45] отклоняется")
  void invalidN() {
    assertThatThrownBy(() -> newSolution().climbStairs(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().climbStairs(-1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().climbStairs(46))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
