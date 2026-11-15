package dev.kata.warmup.week1;

/**
 * Warmup 1.2 — Move Zeroes (LeetCode #283).
 *
 * <h2>Условие</h2>
 *
 * <p>Переместить все нули массива в конец, сохранив относительный порядок ненулевых элементов.
 * Изменять массив нужно in-place, без копии. Минимизируйте число записей: идеальное решение делает
 * одну запись на ненулевой элемент и по одной записи нуля на оставшуюся позицию.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * moveZeroes([0, 1, 0, 3, 12]) → [1, 3, 12, 0, 0]
 * moveZeroes([0])              → [0]
 * moveZeroes([1, 2, 3])        → [1, 2, 3]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code nums.length} ≤ 10⁴;
 *   <li>-2³¹ ≤ {@code nums[i]} ≤ 2³¹ - 1.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1). Два указателя: {@code write} — позиция следующего ненулевого
 * элемента, {@code read} — текущий просматриваемый.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week1.Warmup1_02_MoveZeroesSolution}.
 */
public class Warmup1_02_MoveZeroes {

  /** Сдвигает нули в конец, сохраняя порядок остальных элементов. */
  public void moveZeroes(int[] nums) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
