package dev.kata.solutions.warmup.week3;

import dev.kata.warmup.week3.Warmup3_07_ClimbingStairs;

/**
 * Эталон Warmup 3.7.
 *
 * <p>Рекуррентность {@code f(n) = f(n-1) + f(n-2)} с базой {@code f(1)=1, f(2)=2}. Вместо массива
 * храним два предыдущих значения и обновляем их в цикле. Время O(n), память O(1).
 *
 * <p>Верхняя граница 45 выбрана не случайно: {@code f(46) = 1836311903} всё ещё помещается в {@code
 * int}, а {@code f(47) = 2971215073} уже переполняет знаковый 32-битный тип. Явное исключение лучше
 * молчаливого отрицательного результата.
 */
public final class Warmup3_07_ClimbingStairsSolution extends Warmup3_07_ClimbingStairs {

  private static final int MAX_N = 45;

  @Override
  public int climbStairs(int n) {
    if (n < 1 || n > MAX_N) {
      throw new IllegalArgumentException("n must be in [1, " + MAX_N + "], got " + n);
    }
    if (n <= 2) {
      return n;
    }

    int previous = 1;
    int current = 2;
    for (int step = 3; step <= n; step++) {
      int next = previous + current;
      previous = current;
      current = next;
    }
    return current;
  }
}
