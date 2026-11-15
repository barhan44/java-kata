package dev.kata.solutions.warmup.week3;

import dev.kata.warmup.week3.Warmup3_05_MissingNumber;
import java.util.Objects;

/**
 * Эталон Warmup 3.5.
 *
 * <p>XOR устойчив к переполнению и не требует арифметики с суммой. Начинаем с {@code n} (индекс,
 * которого нет среди элементов), затем для каждого {@code i} ксорим и индекс, и значение. Все
 * присутствующие числа встречаются дважды — как значение и как индекс — и сокращаются; остаётся
 * пропущенное. Пустой массив даёт 0: диапазон {@code [0, 0]}, отсутствует 0.
 */
public final class Warmup3_05_MissingNumberSolution extends Warmup3_05_MissingNumber {

  @Override
  public int missingNumber(int[] nums) {
    Objects.requireNonNull(nums, "nums");

    int missing = nums.length;
    for (int i = 0; i < nums.length; i++) {
      missing ^= i ^ nums[i];
    }
    return missing;
  }
}
