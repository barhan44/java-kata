package dev.kata.warmup.week1;

/**
 * Warmup 1.6 — Majority Element (LeetCode #169).
 *
 * <h2>Условие</h2>
 *
 * <p>Дан непустой массив {@code nums}. Элемент большинства встречается строго больше {@code n/2}
 * раз. Гарантируется, что он существует. Вернуть его за O(n) времени и O(1) дополнительной памяти.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * majorityElement([3,2,3])       → 3
 * majorityElement([2,2,1,1,1,2,2]) → 2
 * majorityElement([1])           → 1
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code nums.length} ≤ 5 × 10⁴;
 *   <li>-10⁹ ≤ {@code nums[i]} ≤ 10⁹;
 *   <li>элемент большинства гарантированно существует.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1) — алгоритм голосования Бойера-Мура: кандидат и счётчик. При {@code
 * count == 0} кандидатом становится текущий элемент; совпадение увеличивает счётчик, несовпадение
 * уменьшает. Кандидат выживает, потому что элемент большинства «перевешивает» все остальные вместе
 * взятые.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week1.Warmup1_06_MajorityElementSolution}.
 */
public class Warmup1_06_MajorityElement {

  /**
   * Возвращает элемент большинства.
   *
   * @throws IllegalArgumentException если массив пуст или элемента большинства нет
   */
  public int majorityElement(int[] nums) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
