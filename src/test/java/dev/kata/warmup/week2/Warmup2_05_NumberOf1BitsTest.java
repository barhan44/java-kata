package dev.kata.warmup.week2;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week2.Warmup2_05_NumberOf1BitsSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 2.5: Number of 1 Bits")
class Warmup2_05_NumberOf1BitsTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup2_05_NumberOf1Bits newSolution() {
    return USE_SOLUTION ? new Warmup2_05_NumberOf1BitsSolution() : new Warmup2_05_NumberOf1Bits();
  }

  @Test
  @DisplayName("Классические примеры")
  void examples() {
    assertThat(newSolution().hammingWeight(11)).isEqualTo(3);
    assertThat(newSolution().hammingWeight(128)).isEqualTo(1);
  }

  @Test
  @DisplayName("Ноль и степени двойки")
  void zeroAndPowersOfTwo() {
    assertThat(newSolution().hammingWeight(0)).isZero();
    assertThat(newSolution().hammingWeight(1)).isEqualTo(1);
    assertThat(newSolution().hammingWeight(1 << 30)).isEqualTo(1);
  }

  @Test
  @DisplayName("Отрицательные значения: беззнаковая интерпретация")
  void negativeValues() {
    assertThat(newSolution().hammingWeight(-1)).isEqualTo(32);
    assertThat(newSolution().hammingWeight(Integer.MIN_VALUE)).isEqualTo(1);
    assertThat(newSolution().hammingWeight(Integer.MAX_VALUE)).isEqualTo(31);
  }

  @Test
  @DisplayName("Чередующиеся биты")
  void alternatingBits() {
    assertThat(newSolution().hammingWeight(0xAAAAAAAA)).isEqualTo(16);
    assertThat(newSolution().hammingWeight(0x55555555)).isEqualTo(16);
  }
}
