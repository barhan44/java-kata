package dev.kata.warmup.week3;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week3.Warmup3_03_PalindromeNumberSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 3.3: Palindrome Number")
class Warmup3_03_PalindromeNumberTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup3_03_PalindromeNumber newSolution() {
    return USE_SOLUTION
        ? new Warmup3_03_PalindromeNumberSolution()
        : new Warmup3_03_PalindromeNumber();
  }

  @Test
  @DisplayName("Классические примеры")
  void examples() {
    assertThat(newSolution().isPalindrome(121)).isTrue();
    assertThat(newSolution().isPalindrome(-121)).isFalse();
    assertThat(newSolution().isPalindrome(10)).isFalse();
  }

  @Test
  @DisplayName("Ноль, одна цифра и чётная длина")
  void shortNumbers() {
    assertThat(newSolution().isPalindrome(0)).isTrue();
    assertThat(newSolution().isPalindrome(7)).isTrue();
    assertThat(newSolution().isPalindrome(11)).isTrue();
    assertThat(newSolution().isPalindrome(1221)).isTrue();
    assertThat(newSolution().isPalindrome(1001)).isTrue();
  }

  @Test
  @DisplayName("Нечётная длина и непалиндромы")
  void longerNumbers() {
    assertThat(newSolution().isPalindrome(12321)).isTrue();
    assertThat(newSolution().isPalindrome(123)).isFalse();
    assertThat(newSolution().isPalindrome(1231)).isFalse();
  }

  @Test
  @DisplayName("Границы int не переполняют разворот")
  void integerBounds() {
    assertThat(newSolution().isPalindrome(Integer.MAX_VALUE)).isFalse();
    assertThat(newSolution().isPalindrome(Integer.MIN_VALUE)).isFalse();
  }
}
