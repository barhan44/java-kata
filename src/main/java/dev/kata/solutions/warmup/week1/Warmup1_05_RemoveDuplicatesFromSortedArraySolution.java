package dev.kata.solutions.warmup.week1;

import dev.kata.warmup.week1.Warmup1_05_RemoveDuplicatesFromSortedArray;
import java.util.Objects;

/**
 * Эталон Warmup 1.5.
 *
 * <p>Классическая техника двух указателей. {@code write} — длина уже сформированного префикса
 * уникальных значений. При просмотре сравниваем текущий элемент с {@code nums[write - 1]}: если
 * отличается, записываем его в позицию {@code write} и увеличиваем счётчик. Сортированность
 * гарантирует, что все повторы идут подряд, поэтому одного сравнения достаточно. HashSet был бы
 * O(n) по памяти и не нужен.
 */
public final class Warmup1_05_RemoveDuplicatesFromSortedArraySolution
    extends Warmup1_05_RemoveDuplicatesFromSortedArray {

  @Override
  public int removeDuplicates(int[] nums) {
    Objects.requireNonNull(nums, "nums");
    if (nums.length == 0) {
      return 0;
    }

    int write = 1;
    for (int read = 1; read < nums.length; read++) {
      if (nums[read] != nums[write - 1]) {
        nums[write++] = nums[read];
      }
    }
    return write;
  }
}
