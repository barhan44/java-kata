package dev.kata.easy;

/**
 * Easy 06. Сбалансированные скобки (ArrayDeque + Map.of).
 *
 * <h2>Задача</h2>
 *
 * <p>Проверить корректность расстановки скобок {@code ()}, {@code []}, {@code {}} в строке. Все
 * остальные символы игнорируются. Каждая открывающая скобка должна закрыться скобкой того же типа в
 * правильном порядке.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * isBalanced("()[]{}")    → true
 * isBalanced("([{}])")    → true
 * isBalanced("(]")        → false
 * isBalanced("a(b)c")     → true
 * isBalanced("(((")       → false
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code input.length()}; строка может содержать любые символы.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(n) в худшем случае (все символы — открывающие скобки). Эталон использует
 * {@code ArrayDeque} как стек (быстрее синхронизированного {@code Stack}) и {@code Map.of} для
 * соответствия закрывающих и открывающих скобок.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy06_BalancedBracketsSolution}.
 */
public class Easy06_BalancedBrackets {

  /** Проверяет баланс скобок, игнорируя остальные символы. */
  public boolean isBalanced(String input) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
