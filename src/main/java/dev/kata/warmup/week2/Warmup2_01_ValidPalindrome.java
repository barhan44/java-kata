package dev.kata.warmup.week2;

/**
 * Warmup 2.1 — Valid Palindrome (LeetCode #125).
 *
 * <h2>Условие</h2>
 *
 * <p>Строка считается палиндромом, если после удаления всех символов, кроме букв и цифр, и
 * приведения регистра к нижнему она читается одинаково слева направо и справа налево. Проверить это
 * без создания новой строки и без регулярных выражений.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * isPalindrome("A man, a plan, a canal: Panama") → true
 * isPalindrome("race a car")                     → false
 * isPalindrome(" ")                              → true
 * isPalindrome("0P")                             → false
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code s.length()} ≤ 2 × 10⁵;
 *   <li>строка содержит ASCII-символы.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1): два указателя пропускают не-алфавитно-цифровые символы. {@code
 * Character.isLetterOrDigit} и {@code Character.toLowerCase} работают без боксинга.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week2.Warmup2_01_ValidPalindromeSolution}.
 */
public class Warmup2_01_ValidPalindrome {

  /** Проверяет палиндром с игнорированием регистра и не-алфавитно-цифровых символов. */
  public boolean isPalindrome(String s) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
