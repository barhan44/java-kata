package dev.kata.solutions.easy;

import dev.kata.easy.Easy18_RunningTotalPrefixSum;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Эталон Easy 18.
 *
 * <p>Сначала строится отсортированная копия — исходный список не меняется. {@code Arrays.setAll}
 * копирует изменения в локальный {@code int[]}, {@code parallelPrefix(Integer::sum)} превращает его
 * в префиксные суммы за O(n). Остаётся соединить суммы с датами через {@code IntStream.range};
 * выходной список неизменяем.
 *
 * <p>Такой подход не тащит мутабельный аккумулятор через лямбды и остаётся линейным после
 * сортировки. При равных датах сортировка стабильна, поэтому элементы сохраняют относительный
 * порядок.
 */
public final class Easy18_RunningTotalPrefixSumSolution extends Easy18_RunningTotalPrefixSum {

  @Override
  public List<BalancePoint> runningTotal(List<DailyDelta> deltas) {
    Objects.requireNonNull(deltas, "deltas");

    List<DailyDelta> sorted =
        deltas.stream().sorted(Comparator.comparing(DailyDelta::date)).toList();

    int[] running = new int[sorted.size()];
    Arrays.setAll(running, index -> sorted.get(index).change());
    Arrays.parallelPrefix(running, Integer::sum);

    return IntStream.range(0, sorted.size())
        .mapToObj(index -> new BalancePoint(sorted.get(index).date(), running[index]))
        .toList();
  }
}
