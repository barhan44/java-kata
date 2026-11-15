package dev.kata.easy;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Easy 18. Кумулятивный баланс (prefix sum).
 *
 * <h2>Задача</h2>
 *
 * <p>Дан список дневных изменений баланса. Отсортировать по дате и вернуть для каждой даты
 * накопленный баланс. Входной список не изменяется; при одинаковых датах сохраняется исходный
 * порядок и обе точки остаются в результате.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * deltas = [DailyDelta(2026-01-03, +10),
 *           DailyDelta(2026-01-01, +5),
 *           DailyDelta(2026-01-02, -2)]
 *
 * runningTotal → [BalancePoint(2026-01-01, 5),
 *                 BalancePoint(2026-01-02, 3),
 *                 BalancePoint(2026-01-03, 13)]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code deltas.size()};
 *   <li>{@code date} не {@code null}; {@code change} — любое {@code int}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n log n) из-за сортировки, память O(n). Эталон сортирует копию, применяет {@code
 * Arrays.parallelPrefix} к локальному массиву изменений и соединяет префиксные суммы с датами через
 * {@code IntStream.range}. Входной список и его элементы не мутируются; результат неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy18_RunningTotalPrefixSumSolution}.
 */
public class Easy18_RunningTotalPrefixSum {

  /** Дневное изменение баланса. */
  public record DailyDelta(LocalDate date, int change) {
    public DailyDelta {
      Objects.requireNonNull(date, "date");
    }
  }

  /** Точка накопленного баланса. */
  public record BalancePoint(LocalDate date, int balance) {
    public BalancePoint {
      Objects.requireNonNull(date, "date");
    }
  }

  /** Возвращает накопленный баланс по отсортированным датам. */
  public List<BalancePoint> runningTotal(List<DailyDelta> deltas) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
