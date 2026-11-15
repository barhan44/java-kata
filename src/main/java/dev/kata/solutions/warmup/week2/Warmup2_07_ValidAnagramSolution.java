package dev.kata.solutions.warmup.week2;

import dev.kata.warmup.week2.Warmup2_07_ValidAnagram;
import java.util.Objects;

/**
 * Эталон Warmup 2.7.
 *
 * <p>Один {@code int[26]} вместо двух map: для строчных латинских букв индекс {@code c - 'a'}
 * однозначен. Увеличение на символах {@code s} и уменьшение на символах {@code t} позволяют
 * обойтись одним массивом. Разные длины отсекаются сразу — иначе один из проходов вышел бы за
 * границы.
 *
 * <p>Сортировка была бы O(n log n) и аллоцировала бы массивы; {@code HashMap} дал бы боксин и
 * лишнюю память.
 */
public final class Warmup2_07_ValidAnagramSolution extends Warmup2_07_ValidAnagram {

  @Override
  public boolean isAnagram(String s, String t) {
    Objects.requireNonNull(s, "s");
    Objects.requireNonNull(t, "t");
    if (s.length() != t.length()) {
      return false;
    }

    int[] counts = new int[26];
    for (int i = 0; i < s.length(); i++) {
      counts[s.charAt(i) - 'a']++;
      counts[t.charAt(i) - 'a']--;
    }

    for (int count : counts) {
      if (count != 0) {
        return false;
      }
    }
    return true;
  }
}
