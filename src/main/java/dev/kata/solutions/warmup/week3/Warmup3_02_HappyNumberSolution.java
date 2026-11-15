package dev.kata.solutions.warmup.week3;

import dev.kata.warmup.week3.Warmup3_02_HappyNumber;

/**
 * Эталон Warmup 3.2.
 *
 * <p>Флойд без множества посещённых: {@code slow} делает один шаг, {@code fast} — два. В
 * функциональном графе «каждое число имеет ровно одного преемника» встреча указателей доказывает
 * цикл; если в цикле оказалась 1, число счастливое. {@code do/while} гарантирует хотя бы один шаг,
 * поэтому {@code isHappy(1)} сразу возвращает true.
 *
 * <p>{@link #squareSum(int)} работает с {@code int}: для входа до 2³¹-1 сумма квадратов цифр
 * максимум 10 × 81 = 810, переполнения нет.
 */
public final class Warmup3_02_HappyNumberSolution extends Warmup3_02_HappyNumber {

  @Override
  public boolean isHappy(int n) {
    if (n < 1) {
      throw new IllegalArgumentException("n must be >= 1, got " + n);
    }

    int slow = n;
    int fast = n;
    do {
      slow = squareSum(slow);
      fast = squareSum(squareSum(fast));
    } while (slow != fast);

    return slow == 1;
  }

  private static int squareSum(int value) {
    int sum = 0;
    while (value > 0) {
      int digit = value % 10;
      sum += digit * digit;
      value /= 10;
    }
    return sum;
  }
}
