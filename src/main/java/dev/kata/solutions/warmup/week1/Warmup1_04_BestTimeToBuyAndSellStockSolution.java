package dev.kata.solutions.warmup.week1;

import dev.kata.warmup.week1.Warmup1_04_BestTimeToBuyAndSellStock;
import java.util.Objects;

/**
 * Эталон Warmup 1.4.
 *
 * <p>Один проход: {@code minPrice} — минимум среди уже просмотренных цен, {@code bestProfit} —
 * максимум разности {@code price - minPrice}. К моменту обработки дня {@code i} минимум всегда
 * относится ко дню не позже {@code i}, поэтому сделка корректна. Пустой массив и монотонное падение
 * дают 0.
 */
public final class Warmup1_04_BestTimeToBuyAndSellStockSolution
    extends Warmup1_04_BestTimeToBuyAndSellStock {

  @Override
  public int maxProfit(int[] prices) {
    Objects.requireNonNull(prices, "prices");

    int minPrice = Integer.MAX_VALUE;
    int bestProfit = 0;
    for (int price : prices) {
      if (price < minPrice) {
        minPrice = price;
      } else {
        bestProfit = Math.max(bestProfit, price - minPrice);
      }
    }
    return bestProfit;
  }
}
