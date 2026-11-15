package dev.kata.warmup.week1;

/**
 * Warmup 1.4 — Best Time to Buy and Sell Stock (LeetCode #121).
 *
 * <h2>Условие</h2>
 *
 * <p>{@code prices[i]} — цена акции в день {@code i}. Разрешена одна покупка и одна продажа позже
 * покупки. Вернуть максимальную прибыль; если прибыли нет, вернуть {@code 0}.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * maxProfit([7,1,5,3,6,4]) → 5   // купить за 1, продать за 6
 * maxProfit([7,6,4,3,1])   → 0   // только падение
 * maxProfit([1,2])         → 1
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code prices.length} ≤ 10⁵;
 *   <li>0 ≤ {@code prices[i]} ≤ 10⁴.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1): поддерживаем минимальную цену слева и лучшую прибыль. Перебор пар —
 * O(n²); разность с максимумом справа тоже O(n), но требует массива или второго прохода.
 *
 * <p>Реализацию см. в {@code
 * dev.kata.solutions.warmup.week1.Warmup1_04_BestTimeToBuyAndSellStockSolution}.
 */
public class Warmup1_04_BestTimeToBuyAndSellStock {

  /** Возвращает максимальную прибыль от одной сделки или 0. */
  public int maxProfit(int[] prices) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
