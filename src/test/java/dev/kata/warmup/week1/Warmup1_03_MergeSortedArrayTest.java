package dev.kata.warmup.week1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week1.Warmup1_03_MergeSortedArraySolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 1.3: Merge Sorted Array")
class Warmup1_03_MergeSortedArrayTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup1_03_MergeSortedArray newSolution() {
    return USE_SOLUTION
        ? new Warmup1_03_MergeSortedArraySolution()
        : new Warmup1_03_MergeSortedArray();
  }

  @Test
  @DisplayName("Классическое слияние")
  void mergesTwoArrays() {
    int[] nums1 = {1, 2, 3, 0, 0, 0};
    newSolution().merge(nums1, 3, new int[] {2, 5, 6}, 3);
    assertThat(nums1).containsExactly(1, 2, 2, 3, 5, 6);
  }

  @Test
  @DisplayName("Второй массив пуст — nums1 не меняется")
  void emptySecondArray() {
    int[] nums1 = {1};
    newSolution().merge(nums1, 1, new int[] {}, 0);
    assertThat(nums1).containsExactly(1);
  }

  @Test
  @DisplayName("Первый массив пуст — nums2 копируется")
  void emptyFirstArray() {
    int[] nums1 = {0};
    newSolution().merge(nums1, 0, new int[] {1}, 1);
    assertThat(nums1).containsExactly(1);
  }

  @Test
  @DisplayName("Дубликаты и отрицательные значения")
  void duplicatesAndNegatives() {
    int[] nums1 = {-3, -1, 0, 0, 0};
    newSolution().merge(nums1, 2, new int[] {-2, -1, 4}, 3);
    assertThat(nums1).containsExactly(-3, -2, -1, -1, 4);
  }

  @Test
  @DisplayName("Некорректные размеры и null")
  void invalidArguments() {
    assertThatThrownBy(() -> newSolution().merge(new int[] {1}, 1, new int[] {2}, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().merge(null, 0, new int[] {}, 0))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().merge(new int[] {}, 0, null, 0))
        .isInstanceOf(NullPointerException.class);
  }
}
