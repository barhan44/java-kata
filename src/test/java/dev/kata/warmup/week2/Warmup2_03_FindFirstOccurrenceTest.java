package dev.kata.warmup.week2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week2.Warmup2_03_FindFirstOccurrenceSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 2.3: Find the Index of the First Occurrence")
class Warmup2_03_FindFirstOccurrenceTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup2_03_FindFirstOccurrence newSolution() {
    return USE_SOLUTION
        ? new Warmup2_03_FindFirstOccurrenceSolution()
        : new Warmup2_03_FindFirstOccurrence();
  }

  @Test
  @DisplayName("Классические примеры")
  void examples() {
    assertThat(newSolution().indexOf("sadbutsad", "sad")).isZero();
    assertThat(newSolution().indexOf("leetcode", "leeto")).isEqualTo(-1);
    assertThat(newSolution().indexOf("mississippi", "issip")).isEqualTo(4);
  }

  @Test
  @DisplayName("Пустая игла и игла длиннее строки")
  void edgeLengths() {
    assertThat(newSolution().indexOf("abc", "")).isZero();
    assertThat(newSolution().indexOf("", "")).isZero();
    assertThat(newSolution().indexOf("a", "abc")).isEqualTo(-1);
    assertThat(newSolution().indexOf("", "a")).isEqualTo(-1);
  }

  @Test
  @DisplayName("Перекрывающиеся вхождения и периодические строки")
  void overlappingPatterns() {
    assertThat(newSolution().indexOf("aaa", "aa")).isZero();
    assertThat(newSolution().indexOf("aaaaa", "bba")).isEqualTo(-1);
    assertThat(newSolution().indexOf("ababca", "abca")).isEqualTo(2);
    assertThat(newSolution().indexOf("aabaabaaa", "aabaaa")).isEqualTo(3);
  }

  @Test
  @DisplayName("Совпадение всей строки и вхождение в конце")
  void exactAndSuffix() {
    assertThat(newSolution().indexOf("abc", "abc")).isZero();
    assertThat(newSolution().indexOf("xxabc", "abc")).isEqualTo(2);
    assertThat(newSolution().indexOf("abcx", "abc")).isZero();
  }

  @Test
  @DisplayName("null-аргументы отклоняются")
  void nullArguments() {
    assertThatThrownBy(() -> newSolution().indexOf(null, "a"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().indexOf("a", null))
        .isInstanceOf(NullPointerException.class);
  }
}
