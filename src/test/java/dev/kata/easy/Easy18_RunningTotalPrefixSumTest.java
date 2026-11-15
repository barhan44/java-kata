package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy18_RunningTotalPrefixSum.BalancePoint;
import dev.kata.easy.Easy18_RunningTotalPrefixSum.DailyDelta;
import dev.kata.solutions.easy.Easy18_RunningTotalPrefixSumSolution;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 18: Running total prefix sum")
class Easy18_RunningTotalPrefixSumTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy18_RunningTotalPrefixSum newSolution() {
    return USE_SOLUTION
        ? new Easy18_RunningTotalPrefixSumSolution()
        : new Easy18_RunningTotalPrefixSum();
  }

  @Test
  @DisplayName("Сортирует даты и считает накопленный баланс")
  void prefixSumsSortedByDate() {
    List<DailyDelta> deltas =
        List.of(
            new DailyDelta(LocalDate.of(2026, 1, 3), 10),
            new DailyDelta(LocalDate.of(2026, 1, 1), 5),
            new DailyDelta(LocalDate.of(2026, 1, 2), -2));

    List<BalancePoint> result = newSolution().runningTotal(deltas);

    assertThat(result)
        .containsExactly(
            new BalancePoint(LocalDate.of(2026, 1, 1), 5),
            new BalancePoint(LocalDate.of(2026, 1, 2), 3),
            new BalancePoint(LocalDate.of(2026, 1, 3), 13));
  }

  @Test
  @DisplayName("Входной список не изменяется")
  void inputIsNotMutated() {
    List<DailyDelta> deltas =
        new ArrayList<>(
            List.of(
                new DailyDelta(LocalDate.of(2026, 1, 2), 1),
                new DailyDelta(LocalDate.of(2026, 1, 1), 2)));
    List<DailyDelta> snapshot = List.copyOf(deltas);

    newSolution().runningTotal(deltas);

    assertThat(deltas).containsExactlyElementsOf(snapshot);
  }

  @Test
  @DisplayName("Отрицательные изменения и переполнение int")
  void negativeAndOverflow() {
    List<BalancePoint> result =
        newSolution()
            .runningTotal(
                List.of(
                    new DailyDelta(LocalDate.of(2026, 1, 1), Integer.MAX_VALUE),
                    new DailyDelta(LocalDate.of(2026, 1, 2), 1)));

    assertThat(result.get(0).balance()).isEqualTo(Integer.MAX_VALUE);
    assertThat(result.get(1).balance()).isEqualTo(Integer.MIN_VALUE);
  }

  @Test
  @DisplayName("Пустой список, один элемент и одинаковые даты")
  void edgeCases() {
    assertThat(newSolution().runningTotal(List.of())).isEmpty();

    assertThat(newSolution().runningTotal(List.of(new DailyDelta(LocalDate.of(2026, 1, 1), 7))))
        .containsExactly(new BalancePoint(LocalDate.of(2026, 1, 1), 7));

    LocalDate sameDay = LocalDate.of(2026, 1, 1);
    assertThat(
            newSolution()
                .runningTotal(List.of(new DailyDelta(sameDay, 1), new DailyDelta(sameDay, 2))))
        .containsExactly(new BalancePoint(sameDay, 1), new BalancePoint(sameDay, 3));
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<BalancePoint> result =
        newSolution().runningTotal(List.of(new DailyDelta(LocalDate.of(2026, 1, 1), 1)));
    assertThatThrownBy(() -> result.add(new BalancePoint(LocalDate.of(2026, 1, 2), 2)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация DailyDelta и null-списка")
  void validation() {
    assertThatThrownBy(() -> new DailyDelta(null, 1)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new BalancePoint(null, 1)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().runningTotal(null))
        .isInstanceOf(NullPointerException.class);
  }
}
