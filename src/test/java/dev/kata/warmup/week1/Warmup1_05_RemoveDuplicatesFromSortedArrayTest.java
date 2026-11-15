package dev.kata.warmup.week1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week1.Warmup1_05_RemoveDuplicatesFromSortedArraySolution;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 1.5: Remove Duplicates from Sorted Array")
class Warmup1_05_RemoveDuplicatesFromSortedArrayTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup1_05_RemoveDuplicatesFromSortedArray newSolution() {
    return USE_SOLUTION
        ? new Warmup1_05_RemoveDuplicatesFromSortedArraySolution()
        : new Warmup1_05_RemoveDuplicatesFromSortedArray();
  }

  @Test
  @DisplayName("Классический пример с длинным массивом")
  void removesDuplicates() {
    int[] nums = {0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
    int k = newSolution().removeDuplicates(nums);

    assertThat(k).isEqualTo(5);
    assertThat(Arrays.copyOf(nums, k)).containsExactly(0, 1, 2, 3, 4);
  }

  @Test
  @DisplayName("Короткий пример")
  void shortArray() {
    int[] nums = {1, 1, 2};
    int k = newSolution().removeDuplicates(nums);

    assertThat(k).isEqualTo(2);
    assertThat(Arrays.copyOf(nums, k)).containsExactly(1, 2);
  }

  @Test
  @DisplayName("Пустой массив и один элемент")
  void emptyAndSingle() {
    assertThat(newSolution().removeDuplicates(new int[] {})).isZero();

    int[] single = {7};
    assertThat(newSolution().removeDuplicates(single)).isEqualTo(1);
    assertThat(single[0]).isEqualTo(7);
  }

  @Test
  @DisplayName("Все элементы одинаковые")
  void allSame() {
    int[] nums = {-2, -2, -2};
    int k = newSolution().removeDuplicates(nums);

    assertThat(k).isEqualTo(1);
    assertThat(nums[0]).isEqualTo(-2);
  }

  @Test
  @DisplayName("null-массив отклоняется")
  void nullArray() {
    assertThatThrownBy(() -> newSolution().removeDuplicates(null))
        .isInstanceOf(NullPointerException.class);
  }
}
