package dev.kata.easy;

/**
 * Easy 25. Максимум в скользящем окне (монотонная очередь).
 *
 * <h2>Задача</h2>
 *
 * <p>Для массива {@code nums} и размера окна {@code k} вернуть массив максимумов каждого окна
 * {@code [i, i + k - 1]}. Требуется O(n), поэтому нельзя пересчитывать максимум каждого окна
 * заново.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * maxSlidingWindow([1,3,-1,-3,5,3,6,7], 3) → [3,3,5,5,6,7]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code nums.length};
 *   <li>1 ≤ {@code k} ≤ {@code nums.length}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(k). Эталон хранит в {@link java.util.ArrayDeque} индексы с убывающими
 * значениями: перед добавлением нового индекса из хвоста удаляются все значения {@code <= nums[i]},
 * из головы — индексы, вышедшие за левую границу окна. Голова очереди всегда указывает на максимум
 * текущего окна. Каждый индекс добавляется и удаляется не более одного раза.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy25_SlidingWindowMaximumDequeSolution}.
 */
public class Easy25_SlidingWindowMaximumDeque {

  /** Возвращает максимум каждого окна размера {@code k}. */
  public int[] maxSlidingWindow(int[] nums, int k) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
