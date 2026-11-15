package dev.kata.warmup.week1;

/**
 * Warmup 1.7 — Contains Duplicate (LeetCode #217).
 *
 * <h2>Условие</h2>
 *
 * <p>Вернуть {@code true}, если в массиве есть хотя бы одно значение, встречающееся дважды, иначе
 * {@code false}. Проверка должна завершаться досрочно при первом найденном повторе.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * containsDuplicate([1,2,3,1]) → true
 * containsDuplicate([1,2,3,4]) → false
 * containsDuplicate([1,1,1,3,3,4,3,2,4,2]) → true
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code nums.length} ≤ 10⁵;
 *   <li>-10⁹ ≤ {@code nums[i]} ≤ 10⁹.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n) в среднем, память O(n): {@code HashSet} и ранний выход. Сортировка дала бы O(n log
 * n) и изменила бы вход; перебор пар — O(n²).
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week1.Warmup1_07_ContainsDuplicateSolution}.
 */
public class Warmup1_07_ContainsDuplicate {

  /** Проверяет наличие повторяющегося значения. */
  public boolean containsDuplicate(int[] nums) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
