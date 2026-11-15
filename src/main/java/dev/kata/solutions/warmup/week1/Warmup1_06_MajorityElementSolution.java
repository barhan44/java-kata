package dev.kata.solutions.warmup.week1;

import dev.kata.warmup.week1.Warmup1_06_MajorityElement;
import java.util.Objects;

/**
 * Эталон Warmup 1.6.
 *
 * <p>Алгоритм Бойера-Мура: кандидат «отменяет» каждое несовпадение. Если элемент встречается больше
 * {@code n/2} раз, он не может быть полностью отменён. Дополнительная проверка кандидата делает
 * решение устойчивым и к входным данным без гарантированного большинства — в этом случае бросаем
 * {@link IllegalArgumentException}, а не возвращаем случайное значение.
 *
 * <p>Счётчик — обычный {@code int} в диапазоне [-n, n], переполнение невозможно.
 */
public final class Warmup1_06_MajorityElementSolution extends Warmup1_06_MajorityElement {

  @Override
  public int majorityElement(int[] nums) {
    Objects.requireNonNull(nums, "nums");
    if (nums.length == 0) {
      throw new IllegalArgumentException("nums must not be empty");
    }

    int candidate = nums[0];
    int count = 0;
    for (int value : nums) {
      if (count == 0) {
        candidate = value;
        count = 1;
      } else if (value == candidate) {
        count++;
      } else {
        count--;
      }
    }

    int occurrences = 0;
    for (int value : nums) {
      if (value == candidate) {
        occurrences++;
      }
    }
    if (occurrences <= nums.length / 2) {
      throw new IllegalArgumentException("No majority element");
    }
    return candidate;
  }
}
