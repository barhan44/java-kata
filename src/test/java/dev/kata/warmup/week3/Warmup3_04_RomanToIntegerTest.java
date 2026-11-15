package dev.kata.warmup.week3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week3.Warmup3_04_RomanToIntegerSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 3.4: Roman to Integer")
class Warmup3_04_RomanToIntegerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup3_04_RomanToInteger newSolution() {
    return USE_SOLUTION ? new Warmup3_04_RomanToIntegerSolution() : new Warmup3_04_RomanToInteger();
  }

  @Test
  @DisplayName("Классические примеры")
  void examples() {
    assertThat(newSolution().romanToInt("III")).isEqualTo(3);
    assertThat(newSolution().romanToInt("LVIII")).isEqualTo(58);
    assertThat(newSolution().romanToInt("MCMXCIV")).isEqualTo(1994);
  }

  @Test
  @DisplayName("Вычитающие комбинации")
  void subtractivePairs() {
    assertThat(newSolution().romanToInt("IV")).isEqualTo(4);
    assertThat(newSolution().romanToInt("IX")).isEqualTo(9);
    assertThat(newSolution().romanToInt("XL")).isEqualTo(40);
    assertThat(newSolution().romanToInt("XC")).isEqualTo(90);
    assertThat(newSolution().romanToInt("CD")).isEqualTo(400);
    assertThat(newSolution().romanToInt("CM")).isEqualTo(900);
  }

  @Test
  @DisplayName("Одиночные символы и пустая строка")
  void singleSymbols() {
    assertThat(newSolution().romanToInt("I")).isEqualTo(1);
    assertThat(newSolution().romanToInt("V")).isEqualTo(5);
    assertThat(newSolution().romanToInt("M")).isEqualTo(1000);
    assertThat(newSolution().romanToInt("")).isZero();
  }

  @Test
  @DisplayName("Некорректный символ и null отклоняются")
  void invalidInput() {
    assertThatThrownBy(() -> newSolution().romanToInt("ABC"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().romanToInt("iv"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().romanToInt(null))
        .isInstanceOf(NullPointerException.class);
  }
}
