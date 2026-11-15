package dev.kata.warmup.week1;

/**
 * Warmup 1.5 — Remove Duplicates from Sorted Array (LeetCode #26).
 *
 * <h2>Условие</h2>
 *
 * <p>Удалить дубликаты из отсортированного массива in-place. Вернуть {@code k} — число уникальных
 * элементов. Первые {@code k} ячеек должны содержать уникальные значения в исходном порядке;
 * остальное содержимое массива не важно. Дополнительную память O(n) использовать нельзя.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * removeDuplicates([1,1,2])             → 2, первые элементы [1,2]
 * removeDuplicates([0,0,1,1,1,2,2,3,3,4]) → 5, [0,1,2,3,4]
 * removeDuplicates([])                  → 0
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code nums.length} ≤ 3 × 10⁴;
 *   <li>массив отсортирован по неубыванию;
 *   <li>-100 ≤ {@code nums[i]} ≤ 100.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1). Указатель {@code write} ставит следующий уникальный элемент; новый
 * элемент отличается от последнего записанного.
 *
 * <p>Реализацию см. в {@code
 * dev.kata.solutions.warmup.week1.Warmup1_05_RemoveDuplicatesFromSortedArraySolution}.
 */
public class Warmup1_05_RemoveDuplicatesFromSortedArray {

  /** Удаляет дубликаты in-place и возвращает новую длину. */
  public int removeDuplicates(int[] nums) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
