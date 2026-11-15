package dev.kata.solutions.warmup.week2;

import dev.kata.warmup.week2.Warmup2_04_SingleNumber;
import java.util.Objects;

/**
 * Эталон Warmup 2.4.
 *
 * <p>Один XOR-аккумулятор. Память не зависит от размера входа, {@code HashSet} не нужен. XOR
 * отрицательных чисел в дополнительном коде работает так же, как для положительных: побитовая
 * операция не интерпретирует знак.
 */
public final class Warmup2_04_SingleNumberSolution extends Warmup2_04_SingleNumber {

  @Override
  public int singleNumber(int[] nums) {
    Objects.requireNonNull(nums, "nums");
    if (nums.length == 0) {
      throw new IllegalArgumentException("nums must not be empty");
    }

    int result = 0;
    for (int value : nums) {
      result ^= value;
    }
    return result;
  }
}
