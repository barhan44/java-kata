package dev.kata.easy;

import java.util.List;

/**
 * Easy 38. Вычисление обратной польской записи.
 *
 * <h2>Задача</h2>
 *
 * <p>Вычислить выражение в RPN (постфиксной записи). Поддерживаются целые числа, {@code +}, {@code
 * -}, {@code *}, {@code /}. Деление целочисленное. Использовать {@link java.util.ArrayDeque} как
 * стек и {@code Map<String, IntBinaryOperator>} для операций.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * evaluateRpn(["2","1","+","3","*"]) → 9
 * evaluateRpn(["4","13","5","/","+"]) → 6
 * evaluateRpn(["5","3","-"])          → 2
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code tokens.size()};
 *   <li>выражение корректно, если не сказано иное; деление на ноль — ошибка.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(n) в худшем случае (все токены — числа). Каждый токен читается один раз,
 * операция снимает два верхних значения и кладёт результат. Ошибки структуры (мало операндов,
 * лишние значения) — {@link IllegalStateException}; неизвестный токен — {@link
 * IllegalArgumentException}.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy38_ExpressionRpnEvaluatorSolution}.
 */
public class Easy38_ExpressionRpnEvaluator {

  /** Вычисляет RPN-выражение. */
  public int evaluateRpn(List<String> tokens) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
