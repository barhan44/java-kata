package dev.kata.warmup.week1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week1.Warmup1_04_BestTimeToBuyAndSellStockSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 1.4: Best Time to Buy and Sell Stock")
class Warmup1_04_BestTimeToBuyAndSellStockTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup1_04_BestTimeToBuyAndSellStock newSolution() {
    return USE_SOLUTION
        ? new Warmup1_04_BestTimeToBuyAndSellStockSolution()
        : new Warmup1_04_BestTimeToBuyAndSellStock();
  }

  @Test
  @DisplayName("Классический пример")
  void findsBestProfit() {
    assertThat(newSolution().maxProfit(new int[] {7, 1, 5, 3, 6, 4})).isEqualTo(5);
  }

  @Test
  @DisplayName("Монотонное падение даёт 0")
  void decreasingPrices() {
    assertThat(newSolution().maxProfit(new int[] {7, 6, 4, 3, 1})).isZero();
  }

  @Test
  @DisplayName("Пустой массив, один день и две цены")
  void smallInputs() {
    assertThat(newSolution().maxProfit(new int[] {})).isZero();
    assertThat(newSolution().maxProfit(new int[] {5})).isZero();
    assertThat(newSolution().maxProfit(new int[] {1, 2})).isEqualTo(1);
  }

  @Test
  @DisplayName("Минимум после максимума не считается сделкой")
  void minimumMustPrecedeMaximum() {
    assertThat(newSolution().maxProfit(new int[] {2, 4, 1})).isEqualTo(2);
  }

  @Test
  @DisplayName("null-массив отклоняется")
  void nullArray() {
    assertThatThrownBy(() -> newSolution().maxProfit(null))
        .isInstanceOf(NullPointerException.class);
  }
}
