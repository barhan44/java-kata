package dev.kata.solutions.warmup.week2;

import dev.kata.warmup.week2.Warmup2_01_ValidPalindrome;
import java.util.Objects;

/**
 * Эталон Warmup 2.1.
 *
 * <p>Два указателя идут навстречу друг другу. Внутренние циклы пропускают всё, что не является
 * буквой или цифрой; наружные сравнивают символы в нижнем регистре. Новая строка не создаётся,
 * поэтому память O(1). Проверка {@code left < right} во внутренних циклах защищает от выхода за
 * границы на строках из одних разделителей.
 */
public final class Warmup2_01_ValidPalindromeSolution extends Warmup2_01_ValidPalindrome {

  @Override
  public boolean isPalindrome(String s) {
    Objects.requireNonNull(s, "s");

    int left = 0;
    int right = s.length() - 1;
    while (left < right) {
      while (left < right && !Character.isLetterOrDigit(s.charAt(left))) {
        left++;
      }
      while (left < right && !Character.isLetterOrDigit(s.charAt(right))) {
        right--;
      }
      if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
        return false;
      }
      left++;
      right--;
    }
    return true;
  }
}
