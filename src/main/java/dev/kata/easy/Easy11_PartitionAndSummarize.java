package dev.kata.easy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Easy 11. Partition + summary за один проход.
 *
 * <h2>Задача</h2>
 *
 * <p>Дан список заказов. За один терминальный {@code collect} посчитать количество и суммарную
 * сумму отдельно для оплаченных и неоплаченных заказов.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * orders = [
 *   Order("1", true,  10.00),
 *   Order("2", false,  5.00),
 *   Order("3", true,  20.00)
 * ]
 *
 * summarize → OrderSummary(
 *   paid   = Summary(count=2, total=30.00),
 *   unpaid = Summary(count=1, total=5.00))
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code orders.size()};
 *   <li>{@code id} не {@code null}/не blank, {@code amount} не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1) на аккумуляторы (не считая результата). Эталон использует {@code
 * Collectors.teeing} с двумя {@code filtering}-ветками: внешний teeing разделяет оплаченные и
 * неоплаченные, внутренний собирает {@code count} и {@code BigDecimal}-сумму. Ручных фильтраций в
 * списки и повторных проходов нет.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy11_PartitionAndSummarizeSolution}.
 */
public class Easy11_PartitionAndSummarize {

  /** Заказ. */
  public record Order(String id, boolean isPaid, BigDecimal amount) {
    public Order {
      Objects.requireNonNull(id, "id");
      if (id.isBlank()) {
        throw new IllegalArgumentException("id must not be blank");
      }
      Objects.requireNonNull(amount, "amount");
    }
  }

  /** Количество и сумма. */
  public record Summary(long count, BigDecimal total) {
    public Summary {
      if (count < 0) {
        throw new IllegalArgumentException("count must be >= 0");
      }
      Objects.requireNonNull(total, "total");
    }
  }

  /** Итоги по оплаченным и неоплаченным заказам. */
  public record OrderSummary(Summary paid, Summary unpaid) {
    public OrderSummary {
      Objects.requireNonNull(paid, "paid");
      Objects.requireNonNull(unpaid, "unpaid");
    }
  }

  /** Считает количество и сумму для обеих групп за один проход. */
  public OrderSummary summarize(List<Order> orders) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
