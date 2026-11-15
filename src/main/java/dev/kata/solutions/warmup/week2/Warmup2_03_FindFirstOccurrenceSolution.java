package dev.kata.solutions.warmup.week2;

import dev.kata.warmup.week2.Warmup2_03_FindFirstOccurrence;
import java.util.Objects;

/**
 * Эталон Warmup 2.3 — KMP.
 *
 * <p>Сначала строится LPS-массив (longest proper prefix which is also suffix) для иглы. При
 * несовпадении указатель иглы откатывается к {@code lps[j - 1]}, а не к нулю, поэтому символы
 * {@code haystack} не перечитываются. Указатель {@code i} монотонно растёт — суммарно O(n + m).
 *
 * <p>Пустая игла обрабатывается отдельно: это единственный случай, когда ответ 0 без поиска.
 */
public final class Warmup2_03_FindFirstOccurrenceSolution extends Warmup2_03_FindFirstOccurrence {

  @Override
  public int indexOf(String haystack, String needle) {
    Objects.requireNonNull(haystack, "haystack");
    Objects.requireNonNull(needle, "needle");

    int n = haystack.length();
    int m = needle.length();
    if (m == 0) {
      return 0;
    }
    if (m > n) {
      return -1;
    }

    int[] lps = buildLps(needle);
    int i = 0;
    int j = 0;
    while (i < n) {
      if (haystack.charAt(i) == needle.charAt(j)) {
        i++;
        j++;
        if (j == m) {
          return i - m;
        }
      } else if (j > 0) {
        j = lps[j - 1];
      } else {
        i++;
      }
    }
    return -1;
  }

  private static int[] buildLps(String pattern) {
    int[] lps = new int[pattern.length()];
    int length = 0;
    int i = 1;
    while (i < pattern.length()) {
      if (pattern.charAt(i) == pattern.charAt(length)) {
        lps[i++] = ++length;
      } else if (length > 0) {
        length = lps[length - 1];
      } else {
        lps[i++] = 0;
      }
    }
    return lps;
  }
}
