package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy11_PartitionAndSummarize.Order;
import dev.kata.easy.Easy11_PartitionAndSummarize.OrderSummary;
import dev.kata.easy.Easy11_PartitionAndSummarize.Summary;
import dev.kata.solutions.easy.Easy11_PartitionAndSummarizeSolution;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 11: Partition and summarize orders")
class Easy11_PartitionAndSummarizeTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy11_PartitionAndSummarize newSolution() {
    return USE_SOLUTION
        ? new Easy11_PartitionAndSummarizeSolution()
        : new Easy11_PartitionAndSummarize();
  }

  @Test
  @DisplayName("Считает количество и сумму обеих групп")
  void summarizesBothGroups() {
    List<Order> orders =
        List.of(
            new Order("1", true, new BigDecimal("10.00")),
            new Order("2", false, new BigDecimal("5.00")),
            new Order("3", true, new BigDecimal("20.00")));

    OrderSummary summary = newSolution().summarize(orders);

    assertThat(summary.paid().count()).isEqualTo(2);
    assertThat(summary.paid().total()).isEqualByComparingTo("30.00");
    assertThat(summary.unpaid().count()).isEqualTo(1);
    assertThat(summary.unpaid().total()).isEqualByComparingTo("5.00");
  }

  @Test
  @DisplayName("Все заказы в одной группе, вторая получает нули")
  void oneSidedGroups() {
    OrderSummary onlyPaid =
        newSolution().summarize(List.of(new Order("1", true, new BigDecimal("7"))));
    assertThat(onlyPaid.paid()).isEqualTo(new Summary(1, new BigDecimal("7")));
    assertThat(onlyPaid.unpaid()).isEqualTo(new Summary(0, BigDecimal.ZERO));

    OrderSummary onlyUnpaid =
        newSolution().summarize(List.of(new Order("2", false, new BigDecimal("3"))));
    assertThat(onlyUnpaid.paid()).isEqualTo(new Summary(0, BigDecimal.ZERO));
    assertThat(onlyUnpaid.unpaid()).isEqualTo(new Summary(1, new BigDecimal("3")));
  }

  @Test
  @DisplayName("Пустой список даёт две нулевые группы")
  void emptyList() {
    OrderSummary summary = newSolution().summarize(List.of());
    assertThat(summary.paid()).isEqualTo(new Summary(0, BigDecimal.ZERO));
    assertThat(summary.unpaid()).isEqualTo(new Summary(0, BigDecimal.ZERO));
  }

  @Test
  @DisplayName("Разный scale BigDecimal складывается корректно")
  void differentScales() {
    List<Order> orders =
        List.of(
            new Order("1", true, new BigDecimal("10.0")),
            new Order("2", true, new BigDecimal("2.50")));

    assertThat(newSolution().summarize(orders).paid().total()).isEqualByComparingTo("12.50");
  }

  @Test
  @DisplayName("Валидация Order, Summary и null-списка")
  void validation() {
    assertThatThrownBy(() -> new Order(null, true, BigDecimal.ONE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Order(" ", true, BigDecimal.ONE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Order("1", true, null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Summary(-1, BigDecimal.ONE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Summary(0, null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().summarize(null))
        .isInstanceOf(NullPointerException.class);
  }
}
