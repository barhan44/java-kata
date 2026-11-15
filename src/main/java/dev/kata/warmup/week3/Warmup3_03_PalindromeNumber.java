package dev.kata.warmup.week3;

/**
 * Warmup 3.3 — Palindrome Number (LeetCode #9).
 *
 * <h2>Условие</h2>
 *
 * <p>Проверить, читается ли целое число одинаково слева направо и справа налево. Отрицательные
 * числа палиндромами не являются из-за знака. Конвертировать число в строку нельзя — нужно
 * разворачивать половину числа.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * isPalindrome(121)  → true
 * isPalindrome(-121) → false
 * isPalindrome(10)   → false
 * isPalindrome(0)    → true
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>-2³¹ ≤ {@code x} ≤ 2³¹ - 1.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(log₁₀ x) — число цифр, память O(1). Разворачиваем только половину: цикл идёт, пока
 * исходное число больше развёрнутой половины. Для чётного числа цифр половины равны, для нечётного
 * средняя цифра отбрасывается делением на 10. Разворот всего числа мог бы переполнить {@code int}.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week3.Warmup3_03_PalindromeNumberSolution}.
 */
public class Warmup3_03_PalindromeNumber {

  /** Проверяет число на палиндром без преобразования в строку. */
  public boolean isPalindrome(int x) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
