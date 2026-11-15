package dev.kata.warmup.week2;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week2.Warmup2_06_ReverseBitsSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 2.6: Reverse Bits")
class Warmup2_06_ReverseBitsTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup2_06_ReverseBits newSolution() {
    return USE_SOLUTION ? new Warmup2_06_ReverseBitsSolution() : new Warmup2_06_ReverseBits();
  }

  @Test
  @DisplayName("Пример LeetCode")
  void leetcodeExample() {
    assertThat(newSolution().reverseBits(43261596)).isEqualTo(964176192);
  }

  @Test
  @DisplayName("Ноль и все единицы")
  void zeroAndAllOnes() {
    assertThat(newSolution().reverseBits(0)).isZero();
    assertThat(newSolution().reverseBits(-1)).isEqualTo(-1);
  }

  @Test
  @DisplayName("Младший бит становится старшим и наоборот")
  void boundaryBits() {
    assertThat(newSolution().reverseBits(1)).isEqualTo(Integer.MIN_VALUE);
    assertThat(newSolution().reverseBits(Integer.MIN_VALUE)).isEqualTo(1);
  }

  @Test
  @DisplayName("Разворот дважды возвращает исходное значение")
  void involution() {
    for (int value : new int[] {0, 1, 42, -123456789, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
      assertThat(newSolution().reverseBits(newSolution().reverseBits(value)))
          .as("value = %d", value)
          .isEqualTo(value);
    }
  }
}
