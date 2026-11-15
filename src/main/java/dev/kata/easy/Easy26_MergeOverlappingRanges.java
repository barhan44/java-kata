package dev.kata.easy;

import java.util.List;

/**
 * Easy 26. Слияние пересекающихся интервалов (ArrayDeque).
 *
 * <h2>Задача</h2>
 *
 * <p>Даны целочисленные интервалы {@code [start, end]}. Слить все пересекающиеся (включая касание в
 * одной точке) интервалы и вернуть результат, отсортированный по началу. Смежные, но не
 * пересекающиеся интервалы, например {@code [1,2]} и {@code [3,4]}, остаются раздельными.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * merge([[1,3],[2,6],[8,10],[15,18]]) → [[1,6],[8,10],[15,18]]
 * merge([[1,4],[4,5]])               → [[1,5]]
 * merge([[1,2],[3,4]])               → [[1,2],[3,4]]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code ranges.size()};
 *   <li>для каждого интервала {@code start ≤ end}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n log n) из-за сортировки, память O(n). После сортировки по началу достаточно
 * сравнивать текущий интервал с последним в {@link java.util.ArrayDeque}; при пересечении последний
 * заменяется объединением. Один проход, без квадратичных проверок. Результат неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy26_MergeOverlappingRangesSolution}.
 */
public class Easy26_MergeOverlappingRanges {

  /** Закрытый целочисленный интервал. */
  public record IntRange(int start, int end) {
    public IntRange {
      if (start > end) {
        throw new IllegalArgumentException(
            "start must be <= end, got [" + start + ", " + end + "]");
      }
    }
  }

  /** Сливает пересекающиеся интервалы. */
  public List<IntRange> merge(List<IntRange> ranges) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
