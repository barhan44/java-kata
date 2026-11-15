package dev.kata.warmup.week2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week2.Warmup2_07_ValidAnagramSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 2.7: Valid Anagram")
class Warmup2_07_ValidAnagramTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup2_07_ValidAnagram newSolution() {
    return USE_SOLUTION ? new Warmup2_07_ValidAnagramSolution() : new Warmup2_07_ValidAnagram();
  }

  @Test
  @DisplayName("Классические примеры")
  void examples() {
    assertThat(newSolution().isAnagram("anagram", "nagaram")).isTrue();
    assertThat(newSolution().isAnagram("rat", "car")).isFalse();
  }

  @Test
  @DisplayName("Разные длины — не анаграммы")
  void differentLengths() {
    assertThat(newSolution().isAnagram("a", "ab")).isFalse();
    assertThat(newSolution().isAnagram("", "a")).isFalse();
    assertThat(newSolution().isAnagram("a", "")).isFalse();
  }

  @Test
  @DisplayName("Пустые строки и одинаковые строки")
  void emptyAndSame() {
    assertThat(newSolution().isAnagram("", "")).isTrue();
    assertThat(newSolution().isAnagram("abc", "abc")).isTrue();
  }

  @Test
  @DisplayName("Одинаковый набор букв с разными кратностями")
  void frequenciesMatter() {
    assertThat(newSolution().isAnagram("aacc", "ccac")).isFalse();
    assertThat(newSolution().isAnagram("aabbcc", "abcabc")).isTrue();
  }

  @Test
  @DisplayName("null-аргументы отклоняются")
  void nullArguments() {
    assertThatThrownBy(() -> newSolution().isAnagram(null, "a"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().isAnagram("a", null))
        .isInstanceOf(NullPointerException.class);
  }
}
