package dev.kata.warmup.week2;

/**
 * Warmup 2.4 — Single Number (LeetCode #136).
 *
 * <h2>Условие</h2>
 *
 * <p>Каждый элемент массива встречается дважды, кроме одного. Найти уникальный элемент за линейное
 * время и O(1) дополнительной памяти.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * singleNumber([2,2,1])     → 1
 * singleNumber([4,1,2,1,2]) → 4
 * singleNumber([1])         → 1
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code nums.length} ≤ 3 × 10⁴;
 *   <li>1 ≤ {@code nums.length} нечётно;
 *   <li>-3 × 10⁴ ≤ {@code nums[i]} ≤ 3 × 10⁴;
 *   <li>каждое значение, кроме одного, встречается ровно дважды.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1): XOR всех элементов. {@code a ^ a == 0}, {@code a ^ 0 == a}, операция
 * коммутативна и ассоциативна, поэтому парные значения взаимно уничтожаются, а уникальное остаётся.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week2.Warmup2_04_SingleNumberSolution}.
 */
public class Warmup2_04_SingleNumber {

  /**
   * Возвращает единственное непарное значение.
   *
   * @throws IllegalArgumentException если массив пуст
   */
  public int singleNumber(int[] nums) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
