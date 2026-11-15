package dev.kata.solutions.warmup.week1;

import dev.kata.warmup.week1.Warmup1_02_MoveZeroes;
import java.util.Objects;

/**
 * Эталон Warmup 1.2.
 *
 * <p>Указатель {@code write} растёт только при встрече ненулевого значения. После одного прохода
 * хвост массива заполняется нулями. Каждый ненулевой элемент записывается ровно один раз; нулевые
 * элементы, уже стоящие в хвосте, повторно не пишутся. Обмены (swap) при каждом нуле дали бы больше
 * записей и не нужны для условия задачи.
 */
public final class Warmup1_02_MoveZeroesSolution extends Warmup1_02_MoveZeroes {

  @Override
  public void moveZeroes(int[] nums) {
    Objects.requireNonNull(nums, "nums");

    int write = 0;
    for (int read = 0; read < nums.length; read++) {
      if (nums[read] != 0) {
        nums[write++] = nums[read];
      }
    }
    while (write < nums.length) {
      nums[write++] = 0;
    }
  }
}
