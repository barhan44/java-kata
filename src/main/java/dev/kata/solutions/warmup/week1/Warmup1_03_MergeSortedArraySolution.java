package dev.kata.solutions.warmup.week1;

import dev.kata.warmup.week1.Warmup1_03_MergeSortedArray;
import java.util.Objects;

/**
 * Эталон Warmup 1.3.
 *
 * <p>Три указателя: {@code i = m - 1}, {@code j = n - 1}, {@code write = m + n - 1}. На каждом шаге
 * больший из хвостов пишется в конец буфера. Когда {@code nums2} закончился, оставшийся префикс
 * {@code nums1} уже стоит на своих местах — копировать его не нужно. Поэтому внешний цикл идёт по
 * {@code j}, а не по обоим индексам.
 */
public final class Warmup1_03_MergeSortedArraySolution extends Warmup1_03_MergeSortedArray {

  @Override
  public void merge(int[] nums1, int m, int[] nums2, int n) {
    Objects.requireNonNull(nums1, "nums1");
    Objects.requireNonNull(nums2, "nums2");
    if (m < 0 || n < 0 || m + n > nums1.length || n > nums2.length) {
      throw new IllegalArgumentException("Invalid m/n or array lengths");
    }

    int i = m - 1;
    int j = n - 1;
    int write = m + n - 1;

    while (j >= 0) {
      if (i >= 0 && nums1[i] > nums2[j]) {
        nums1[write--] = nums1[i--];
      } else {
        nums1[write--] = nums2[j--];
      }
    }
  }
}
