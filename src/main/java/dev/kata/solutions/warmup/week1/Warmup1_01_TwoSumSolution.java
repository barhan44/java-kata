package dev.kata.solutions.warmup.week1;

import dev.kata.warmup.week1.Warmup1_01_TwoSum;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Эталон Warmup 1.1.
 *
 * <p>Один проход: для текущего значения ищем в map дополнение {@code target - nums[i]}. Если оно
 * уже встречалось, возвращаем пару индексов. Хранить значение → индекс выгоднее, чем сортировать
 * массив: сортировка O(n log n) ломает исходные индексы и требует копии.
 *
 * <p>Дополнение считается в {@code long}: разность {@code target - nums[i]} может выйти за границы
 * {@code int} (например, {@code Integer.MIN_VALUE - 1}). Такое значение заведомо не найдётся в
 * массиве {@code int}, поэтому его можно пропустить.
 */
public final class Warmup1_01_TwoSumSolution extends Warmup1_01_TwoSum {

  @Override
  public int[] twoSum(int[] nums, int target) {
    Objects.requireNonNull(nums, "nums");

    Map<Integer, Integer> indexByValue = new HashMap<>();
    for (int i = 0; i < nums.length; i++) {
      long needed = (long) target - nums[i];
      if (needed >= Integer.MIN_VALUE && needed <= Integer.MAX_VALUE) {
        Integer previous = indexByValue.get((int) needed);
        if (previous != null) {
          return new int[] {previous, i};
        }
      }
      indexByValue.put(nums[i], i);
    }
    throw new IllegalArgumentException("No two elements sum to " + target);
  }
}
