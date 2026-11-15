package dev.kata.warmup.week2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week2.Warmup2_02_ReverseStringSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 2.2: Reverse String")
class Warmup2_02_ReverseStringTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup2_02_ReverseString newSolution() {
    return USE_SOLUTION ? new Warmup2_02_ReverseStringSolution() : new Warmup2_02_ReverseString();
  }

  @Test
  @DisplayName("Нечётная длина")
  void oddLength() {
    char[] s = {'h', 'e', 'l', 'l', 'o'};
    newSolution().reverseString(s);
    assertThat(s).containsExactly('o', 'l', 'l', 'e', 'h');
  }

  @Test
  @DisplayName("Чётная длина")
  void evenLength() {
    char[] s = {'H', 'a', 'n', 'n', 'a', 'h'};
    newSolution().reverseString(s);
    assertThat(s).containsExactly('h', 'a', 'n', 'n', 'a', 'H');
  }

  @Test
  @DisplayName("Пустой массив и один символ")
  void emptyAndSingle() {
    char[] empty = {};
    newSolution().reverseString(empty);
    assertThat(empty).isEmpty();

    char[] single = {'x'};
    newSolution().reverseString(single);
    assertThat(single).containsExactly('x');
  }

  @Test
  @DisplayName("Изменяется исходный массив")
  void inPlace() {
    char[] s = {'a', 'b'};
    char[] reference = s;
    newSolution().reverseString(s);
    assertThat(reference).containsExactly('b', 'a');
  }

  @Test
  @DisplayName("null-массив отклоняется")
  void nullArray() {
    assertThatThrownBy(() -> newSolution().reverseString(null))
        .isInstanceOf(NullPointerException.class);
  }
}
