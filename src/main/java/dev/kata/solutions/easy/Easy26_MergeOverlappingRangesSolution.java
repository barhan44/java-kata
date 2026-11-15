package dev.kata.solutions.easy;

import dev.kata.easy.Easy26_MergeOverlappingRanges;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Эталон Easy 26.
 *
 * <p>После сортировки по {@code start}, затем {@code end} все пересекающиеся интервалы идут подряд.
 * {@link ArrayDeque} играет роль стека объединённых интервалов: если текущий начинается позже конца
 * последнего, он не пересекается и добавляется в хвост. Иначе последний заменяется интервалом с
 * началом последнего и максимумом концов. Проверка {@code start <= last.end()} означает, что
 * касание в одной точке тоже сливается.
 *
 * <p>{@code List.copyOf} отдаёт неизменяемый список; входной список не сортируется и не меняется.
 */
public final class Easy26_MergeOverlappingRangesSolution extends Easy26_MergeOverlappingRanges {

  @Override
  public List<IntRange> merge(List<IntRange> ranges) {
    Objects.requireNonNull(ranges, "ranges");

    List<IntRange> sorted =
        ranges.stream()
            .sorted(Comparator.comparingInt(IntRange::start).thenComparingInt(IntRange::end))
            .toList();

    Deque<IntRange> merged = new ArrayDeque<>();
    for (IntRange range : sorted) {
      IntRange last = merged.peekLast();
      if (last == null || range.start() > last.end()) {
        merged.addLast(range);
      } else {
        merged.removeLast();
        merged.addLast(new IntRange(last.start(), Math.max(last.end(), range.end())));
      }
    }
    return List.copyOf(merged);
  }
}
