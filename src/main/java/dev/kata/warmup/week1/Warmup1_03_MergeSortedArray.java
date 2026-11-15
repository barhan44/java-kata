package dev.kata.warmup.week1;

/**
 * Warmup 1.3 — Merge Sorted Array (LeetCode #88).
 *
 * <h2>Условие</h2>
 *
 * <p>{@code nums1} имеет длину {@code m + n}, первые {@code m} элементов отсортированы, остальные
 * равны нулю и являются буфером. {@code nums2} содержит {@code n} отсортированных элементов. Слить
 * массивы в {@code nums1} in-place, сохранив отсортированность.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * merge([1,2,3,0,0,0], 3, [2,5,6], 3) → [1,2,2,3,5,6]
 * merge([1], 1, [], 0)                → [1]
 * merge([0], 0, [1], 1)               → [1]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code m}, {@code n} ≤ 200;
 *   <li>{@code nums1.length == m + n}, {@code nums2.length == n};
 *   <li>-10⁹ ≤ элементы ≤ 10⁹.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(m + n), память O(1). Слияние с конца: сравниваем хвосты и пишем в свободный конец
 * {@code nums1}, поэтому ещё не прочитанные элементы не затираются. Слияние с начала потребовало бы
 * сдвигов или копии.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week1.Warmup1_03_MergeSortedArraySolution}.
 */
public class Warmup1_03_MergeSortedArray {

  /** Сливает {@code nums2[0..n)} в {@code nums1[0..m+n)} in-place. */
  public void merge(int[] nums1, int m, int[] nums2, int n) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
