package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy06_BalancedBracketsSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 06: Balanced brackets")
class Easy06_BalancedBracketsTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy06_BalancedBrackets newSolution() {
    return USE_SOLUTION ? new Easy06_BalancedBracketsSolution() : new Easy06_BalancedBrackets();
  }

  @Test
  @DisplayName("Простые и вложенные корректные последовательности")
  void balancedInputs() {
    assertThat(newSolution().isBalanced("()")).isTrue();
    assertThat(newSolution().isBalanced("()[]{}")).isTrue();
    assertThat(newSolution().isBalanced("([{}])")).isTrue();
    assertThat(newSolution().isBalanced("{[()()]}")).isTrue();
  }

  @Test
  @DisplayName("Неверный тип закрывающей скобки")
  void mismatchedTypes() {
    assertThat(newSolution().isBalanced("(]")).isFalse();
    assertThat(newSolution().isBalanced("([)]")).isFalse();
    assertThat(newSolution().isBalanced("{]")).isFalse();
  }

  @Test
  @DisplayName("Незакрытые и лишние закрывающие скобки")
  void unbalancedCounts() {
    assertThat(newSolution().isBalanced("(((")).isFalse();
    assertThat(newSolution().isBalanced(")))")).isFalse();
    assertThat(newSolution().isBalanced("())")).isFalse();
    assertThat(newSolution().isBalanced("(()")).isFalse();
  }

  @Test
  @DisplayName("Остальные символы игнорируются")
  void ignoresOtherCharacters() {
    assertThat(newSolution().isBalanced("a(b)c")).isTrue();
    assertThat(newSolution().isBalanced("if (x[0] > 0) { y(); }")).isTrue();
    assertThat(newSolution().isBalanced("123")).isTrue();
    assertThat(newSolution().isBalanced("a(b]c")).isFalse();
  }

  @Test
  @DisplayName("Пустая строка и null")
  void emptyAndNull() {
    assertThat(newSolution().isBalanced("")).isTrue();
    assertThatThrownBy(() -> newSolution().isBalanced(null))
        .isInstanceOf(NullPointerException.class);
  }
}
