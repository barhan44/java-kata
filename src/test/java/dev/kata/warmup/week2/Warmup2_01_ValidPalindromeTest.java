package dev.kata.warmup.week2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week2.Warmup2_01_ValidPalindromeSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 2.1: Valid Palindrome")
class Warmup2_01_ValidPalindromeTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup2_01_ValidPalindrome newSolution() {
    return USE_SOLUTION
        ? new Warmup2_01_ValidPalindromeSolution()
        : new Warmup2_01_ValidPalindrome();
  }

  @Test
  @DisplayName("Классические примеры")
  void examples() {
    assertThat(newSolution().isPalindrome("A man, a plan, a canal: Panama")).isTrue();
    assertThat(newSolution().isPalindrome("race a car")).isFalse();
    assertThat(newSolution().isPalindrome("0P")).isFalse();
  }

  @Test
  @DisplayName("Пустая строка и только разделители — палиндром")
  void emptyAndPunctuation() {
    assertThat(newSolution().isPalindrome("")).isTrue();
    assertThat(newSolution().isPalindrome(" ")).isTrue();
    assertThat(newSolution().isPalindrome(".,;:!?")).isTrue();
  }

  @Test
  @DisplayName("Цифры и смешанный регистр")
  void digitsAndCase() {
    assertThat(newSolution().isPalindrome("12321")).isTrue();
    assertThat(newSolution().isPalindrome("1231")).isFalse();
    assertThat(newSolution().isPalindrome("Aa")).isTrue();
    assertThat(newSolution().isPalindrome("abBa")).isTrue();
  }

  @Test
  @DisplayName("null-строка отклоняется")
  void nullString() {
    assertThatThrownBy(() -> newSolution().isPalindrome(null))
        .isInstanceOf(NullPointerException.class);
  }
}
