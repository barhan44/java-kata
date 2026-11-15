package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy38_ExpressionRpnEvaluatorSolution;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 38: RPN evaluator")
class Easy38_ExpressionRpnEvaluatorTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy38_ExpressionRpnEvaluator newSolution() {
    return USE_SOLUTION
        ? new Easy38_ExpressionRpnEvaluatorSolution()
        : new Easy38_ExpressionRpnEvaluator();
  }

  @Test
  @DisplayName("Классические примеры и порядок операндов")
  void evaluatesExpressions() {
    assertThat(newSolution().evaluateRpn(List.of("2", "1", "+", "3", "*"))).isEqualTo(9);
    assertThat(newSolution().evaluateRpn(List.of("4", "13", "5", "/", "+"))).isEqualTo(6);
    assertThat(newSolution().evaluateRpn(List.of("5", "3", "-"))).isEqualTo(2);
    assertThat(newSolution().evaluateRpn(List.of("15", "3", "/"))).isEqualTo(5);
  }

  @Test
  @DisplayName("Длинное выражение LeetCode")
  void longExpression() {
    List<String> tokens =
        List.of("10", "6", "9", "3", "+", "-11", "*", "/", "*", "17", "+", "5", "+");
    assertThat(newSolution().evaluateRpn(tokens)).isEqualTo(22);
  }

  @Test
  @DisplayName("Один операнд")
  void singleOperand() {
    assertThat(newSolution().evaluateRpn(List.of("42"))).isEqualTo(42);
    assertThat(newSolution().evaluateRpn(List.of("-7"))).isEqualTo(-7);
  }

  @Test
  @DisplayName("Некорректная структура выражения")
  void malformedExpressions() {
    assertThatThrownBy(() -> newSolution().evaluateRpn(List.of()))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> newSolution().evaluateRpn(List.of("+")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> newSolution().evaluateRpn(List.of("1", "2")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> newSolution().evaluateRpn(List.of("1", "2", "+", "3")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Неизвестный токен и null-аргументы")
  void invalidTokens() {
    assertThatThrownBy(() -> newSolution().evaluateRpn(List.of("1", "2", "^")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().evaluateRpn(List.of(" ")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().evaluateRpn(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().evaluateRpn(Arrays.asList("1", null, "+")))
        .isInstanceOf(NullPointerException.class);
  }
}
