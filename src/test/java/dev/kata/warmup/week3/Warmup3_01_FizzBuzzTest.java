package dev.kata.warmup.week3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week3.Warmup3_01_FizzBuzzSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 3.1: FizzBuzz")
class Warmup3_01_FizzBuzzTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup3_01_FizzBuzz newSolution() {
    return USE_SOLUTION ? new Warmup3_01_FizzBuzzSolution() : new Warmup3_01_FizzBuzz();
  }

  @Test
  @DisplayName("Полная последовательность до 15")
  void fullSequence() {
    assertThat(newSolution().fizzBuzz(15))
        .containsExactly(
            "1",
            "2",
            "Fizz",
            "4",
            "Buzz",
            "Fizz",
            "7",
            "8",
            "Fizz",
            "Buzz",
            "11",
            "Fizz",
            "13",
            "14",
            "FizzBuzz");
  }

  @Test
  @DisplayName("Только Fizz, только Buzz и FizzBuzz")
  void divisibilityRules() {
    assertThat(newSolution().fizzBuzz(3)).containsExactly("1", "2", "Fizz");
    assertThat(newSolution().fizzBuzz(5)).containsExactly("1", "2", "Fizz", "4", "Buzz");
    assertThat(newSolution().fizzBuzz(15).get(14)).isEqualTo("FizzBuzz");
  }

  @Test
  @DisplayName("n = 1")
  void one() {
    assertThat(newSolution().fizzBuzz(1)).containsExactly("1");
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<String> result = newSolution().fizzBuzz(3);
    assertThatThrownBy(() -> result.add("x")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("n < 1 отклоняется")
  void invalidN() {
    assertThatThrownBy(() -> newSolution().fizzBuzz(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().fizzBuzz(-5))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
