package dev.kata.solutions.warmup.week2;

import dev.kata.warmup.week2.Warmup2_02_ReverseString;
import java.util.Objects;

/**
 * Эталон Warmup 2.2.
 *
 * <p>Классический two-pointer swap. Средний элемент нечётной длины остаётся на месте, поэтому цикл
 * идёт, пока {@code left < right}. Никаких промежуточных массивов или {@code StringBuilder}.
 */
public final class Warmup2_02_ReverseStringSolution extends Warmup2_02_ReverseString {

  @Override
  public void reverseString(char[] s) {
    Objects.requireNonNull(s, "s");

    int left = 0;
    int right = s.length - 1;
    while (left < right) {
      char temp = s[left];
      s[left] = s[right];
      s[right] = temp;
      left++;
      right--;
    }
  }
}
