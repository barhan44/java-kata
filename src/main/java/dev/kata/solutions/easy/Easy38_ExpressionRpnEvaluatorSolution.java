package dev.kata.solutions.easy;

import dev.kata.easy.Easy38_ExpressionRpnEvaluator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntBinaryOperator;

/**
 * Эталон Easy 38.
 *
 * <p>Стек — {@link ArrayDeque}: {@code push} кладёт число или результат, {@code pop} снимает правый
 * и левый операнды именно в этом порядке. Операции хранятся в {@code Map<String,
 * IntBinaryOperator>}, поэтому обработка токена — это одна проверка {@code OPERATORS.get(token)}
 * без длинного {@code if/else}. Неизвестный токен пытается разобраться как число; ошибка разбора
 * оборачивается в {@link IllegalArgumentException}.
 *
 * <p>После обработки в стеке обязан остаться ровно один элемент. Иначе выражение структурно
 * некорректно: операндов не хватило или остались лишние.
 */
public final class Easy38_ExpressionRpnEvaluatorSolution extends Easy38_ExpressionRpnEvaluator {

  private static final Map<String, IntBinaryOperator> OPERATORS =
      Map.of(
          "+", (left, right) -> left + right,
          "-", (left, right) -> left - right,
          "*", (left, right) -> left * right,
          "/", (left, right) -> left / right);

  @Override
  public int evaluateRpn(List<String> tokens) {
    Objects.requireNonNull(tokens, "tokens");

    Deque<Integer> stack = new ArrayDeque<>();
    for (String token : tokens) {
      Objects.requireNonNull(token, "token");
      IntBinaryOperator operator = OPERATORS.get(token);
      if (operator == null) {
        try {
          stack.push(Integer.parseInt(token));
        } catch (NumberFormatException error) {
          throw new IllegalArgumentException("Invalid RPN token: " + token, error);
        }
      } else {
        if (stack.size() < 2) {
          throw new IllegalStateException("Not enough operands for " + token);
        }
        int right = stack.pop();
        int left = stack.pop();
        stack.push(operator.applyAsInt(left, right));
      }
    }

    if (stack.size() != 1) {
      throw new IllegalStateException("Malformed RPN expression: stack size " + stack.size());
    }
    return stack.pop();
  }
}
