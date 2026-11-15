package dev.kata.solutions.warmup.week3;

import dev.kata.warmup.week3.Warmup3_06_IsomorphicStrings;
import java.util.Objects;

/**
 * Эталон Warmup 3.6.
 *
 * <p>Два массива по 256 элементов хранят не сами символы, а последнюю позицию, на которой символ
 * встречался + 1 (0 — «ещё не встречался»). Если текущие позиции двух символов различны, значит,
 * один из них уже был связан с другим партнёром — строки неизоморфны. Обновление обеих записей
 * одинаковым {@code i + 1} фиксирует связь на текущем шаге.
 *
 * <p>Альтернатива с {@code HashMap} дала бы боксин; массивы дают O(1) памяти и предсказуемость.
 */
public final class Warmup3_06_IsomorphicStringsSolution extends Warmup3_06_IsomorphicStrings {

  private static final int ASCII_SIZE = 256;

  @Override
  public boolean isIsomorphic(String s, String t) {
    Objects.requireNonNull(s, "s");
    Objects.requireNonNull(t, "t");
    if (s.length() != t.length()) {
      return false;
    }

    int[] lastSeenS = new int[ASCII_SIZE];
    int[] lastSeenT = new int[ASCII_SIZE];
    for (int i = 0; i < s.length(); i++) {
      int sIndex = s.charAt(i);
      int tIndex = t.charAt(i);
      if (lastSeenS[sIndex] != lastSeenT[tIndex]) {
        return false;
      }
      lastSeenS[sIndex] = i + 1;
      lastSeenT[tIndex] = i + 1;
    }
    return true;
  }
}
