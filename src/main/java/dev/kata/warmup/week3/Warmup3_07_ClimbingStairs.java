package dev.kata.warmup.week3;

/**
 * Warmup 3.7 — Climbing Stairs (LeetCode #70).
 *
 * <h2>Условие</h2>
 *
 * <p>Подъём по лестнице: за шаг можно подняться на 1 или 2 ступени. Сколько различных способов
 * подняться на {@code n} ступеней? Реализовать динамическое программирование за O(n) по времени и
 * O(1) по памяти.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * climbStairs(1) → 1
 * climbStairs(2) → 2   // 1+1, 2
 * climbStairs(3) → 3   // 1+1+1, 1+2, 2+1
 * climbStairs(5) → 8
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code n} ≤ 45 — ответ помещается в {@code int}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1). Количество способов подняться на ступень {@code i} равно сумме
 * способов для {@code i-1} и {@code i-2} — это числа Фибоначчи со сдвигом. Храним только два
 * предыдущих значения; массив DP на n ячеек не нужен.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week3.Warmup3_07_ClimbingStairsSolution}.
 */
public class Warmup3_07_ClimbingStairs {

  /**
   * Возвращает число способов подняться на {@code n} ступеней.
   *
   * @throws IllegalArgumentException если {@code n} вне [1, 45]
   */
  public int climbStairs(int n) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
