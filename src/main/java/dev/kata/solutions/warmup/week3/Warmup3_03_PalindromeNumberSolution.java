package dev.kata.solutions.warmup.week3;

import dev.kata.warmup.week3.Warmup3_03_PalindromeNumber;

/**
 * Эталон Warmup 3.3.
 *
 * <p>Отрицательные числа и положительные, оканчивающиеся на 0 (кроме самого 0), сразу отсекаются:
 * палиндром не может начинаться с нуля. Затем разворачиваем младшие цифры в {@code reversedHalf},
 * пока {@code x > reversedHalf}. Условие завершения означает, что развёрнута ровно половина.
 *
 * <p>Для чётной длины {@code x == reversedHalf}; для нечётной средняя цифра находится в {@code
 * reversedHalf} и убирается {@code / 10}. Переполнения нет: разворачивается не больше половины цифр
 * исходного числа.
 */
public final class Warmup3_03_PalindromeNumberSolution extends Warmup3_03_PalindromeNumber {

  @Override
  public boolean isPalindrome(int x) {
    if (x < 0 || (x % 10 == 0 && x != 0)) {
      return false;
    }

    int reversedHalf = 0;
    while (x > reversedHalf) {
      reversedHalf = reversedHalf * 10 + x % 10;
      x /= 10;
    }
    return x == reversedHalf || x == reversedHalf / 10;
  }
}
