package dev.kata.solutions.easy;

import dev.kata.easy.Easy11_PartitionAndSummarize;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Эталон Easy 11.
 *
 * <p>Внутренний teeing собирает {@code Summary} из двух независимых downstream: {@code counting()}
 * и {@code mapping(amount, reducing(ZERO, add))}. Внешний teeing применяет один и тот же коллектор
 * к двум {@code filtering}-веткам — оплаченной и неоплаченной. JDK передаёт каждый элемент обоим
 * downstream, поэтому поток читается ровно один раз.
 *
 * <p>{@code BigDecimal.ZERO} — корректная нейтральная сумма: пустая группа даёт {@code 0}, а не
 * {@code null}. Возвращаемые records неизменяемы.
 */
public final class Easy11_PartitionAndSummarizeSolution extends Easy11_PartitionAndSummarize {

  private static final Collector<Order, ?, Summary> SUMMARY_COLLECTOR =
      Collectors.teeing(
          Collectors.counting(),
          Collectors.mapping(Order::amount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)),
          Summary::new);

  @Override
  public OrderSummary summarize(List<Order> orders) {
    Objects.requireNonNull(orders, "orders");

    return orders.stream()
        .collect(
            Collectors.teeing(
                Collectors.filtering(Order::isPaid, SUMMARY_COLLECTOR),
                Collectors.filtering(order -> !order.isPaid(), SUMMARY_COLLECTOR),
                OrderSummary::new));
  }
}
