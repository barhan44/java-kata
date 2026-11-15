package dev.kata.warmup.week1;

/**
 * Warmup 1.1 — Two Sum (LeetCode #1).
 *
 * <h2>Условие</h2>
 *
 * <p>Дан целочисленный массив {@code nums} и {@code target}. Вернуть индексы двух элементов, сумма
 * которых равна {@code target}. Гарантируется ровно одно решение, один и тот же элемент
 * использовать дважды нельзя. Индексы можно вернуть в любом порядке.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * twoSum([2, 7, 11, 15], 9) → [0, 1]
 * twoSum([3, 2, 4], 6)      → [1, 2]
 * twoSum([3, 3], 6)         → [0, 1]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>2 ≤ {@code nums.length} ≤ 10⁴;
 *   <li>-10⁹ ≤ {@code nums[i]}, {@code target} ≤ 10⁹.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(n): один проход с {@code HashMap} «значение → индекс». Наивные два цикла
 * — O(n²). При поиске дополнения учитывать возможное переполнение {@code int}.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week1.Warmup1_01_TwoSumSolution}.
 */
public class Warmup1_01_TwoSum {

  /**
   * Возвращает индексы двух чисел, дающих в сумме {@code target}.
   *
   * @throws IllegalArgumentException если решения нет
   */
  public int[] twoSum(int[] nums, int target) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
