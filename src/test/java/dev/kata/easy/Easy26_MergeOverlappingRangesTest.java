package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy26_MergeOverlappingRanges.IntRange;
import dev.kata.solutions.easy.Easy26_MergeOverlappingRangesSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 26: Merge overlapping ranges")
class Easy26_MergeOverlappingRangesTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy26_MergeOverlappingRanges newSolution() {
    return USE_SOLUTION
        ? new Easy26_MergeOverlappingRangesSolution()
        : new Easy26_MergeOverlappingRanges();
  }

  @Test
  @DisplayName("Классический пример")
  void classicExample() {
    assertThat(
            newSolution()
                .merge(
                    List.of(
                        new IntRange(1, 3),
                        new IntRange(2, 6),
                        new IntRange(8, 10),
                        new IntRange(15, 18))))
        .containsExactly(new IntRange(1, 6), new IntRange(8, 10), new IntRange(15, 18));
  }

  @Test
  @DisplayName("Вложенный интервал и касание в точке")
  void nestedAndTouching() {
    assertThat(
            newSolution()
                .merge(List.of(new IntRange(1, 10), new IntRange(2, 3), new IntRange(4, 5))))
        .containsExactly(new IntRange(1, 10));
    assertThat(newSolution().merge(List.of(new IntRange(1, 4), new IntRange(4, 5))))
        .containsExactly(new IntRange(1, 5));
  }

  @Test
  @DisplayName("Смежные, но не пересекающиеся интервалы не сливаются")
  void adjacentRangesStaySeparate() {
    assertThat(newSolution().merge(List.of(new IntRange(1, 2), new IntRange(3, 4))))
        .containsExactly(new IntRange(1, 2), new IntRange(3, 4));
  }

  @Test
  @DisplayName("Несортированный вход, дубликаты и один интервал")
  void unsortedDuplicatesAndSingle() {
    assertThat(
            newSolution()
                .merge(
                    List.of(
                        new IntRange(10, 12),
                        new IntRange(1, 3),
                        new IntRange(2, 4),
                        new IntRange(10, 12))))
        .containsExactly(new IntRange(1, 4), new IntRange(10, 12));
    assertThat(newSolution().merge(List.of(new IntRange(5, 7))))
        .containsExactly(new IntRange(5, 7));
  }

  @Test
  @DisplayName("Пустой список, вход не меняется, результат неизменяем")
  void emptyAndImmutability() {
    assertThat(newSolution().merge(List.of())).isEmpty();

    List<IntRange> input =
        new java.util.ArrayList<>(List.of(new IntRange(2, 3), new IntRange(1, 2)));
    List<IntRange> result = newSolution().merge(input);

    assertThat(input).containsExactly(new IntRange(2, 3), new IntRange(1, 2));
    assertThat(result).containsExactly(new IntRange(1, 3));
    assertThatThrownBy(() -> result.add(new IntRange(4, 5)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация IntRange и null-списка")
  void validation() {
    assertThatThrownBy(() -> new IntRange(5, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().merge(null)).isInstanceOf(NullPointerException.class);
  }
}
