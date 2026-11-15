package dev.kata.solutions.warmup.week1;

import dev.kata.warmup.week1.Warmup1_07_ContainsDuplicate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Эталон Warmup 1.7.
 *
 * <p>Добавление в {@code HashSet} возвращает {@code false}, если элемент уже был. На этом же шаге
 * можно вернуть {@code true}, не просматривая остаток массива. Начальная ёмкость не задаётся
 * вручную: {@code HashSet} всё равно вырастет, а для малых массивов лишняя память вредна.
 */
public final class Warmup1_07_ContainsDuplicateSolution extends Warmup1_07_ContainsDuplicate {

  @Override
  public boolean containsDuplicate(int[] nums) {
    Objects.requireNonNull(nums, "nums");

    Set<Integer> seen = new HashSet<>();
    for (int value : nums) {
      if (!seen.add(value)) {
        return true;
      }
    }
    return false;
  }
}
