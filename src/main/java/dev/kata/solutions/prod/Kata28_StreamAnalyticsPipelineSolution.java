package dev.kata.solutions.prod;

import dev.kata.prod.Kata28_StreamAnalyticsPipeline;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Эталонное решение Kata 28.
 *
 * <h2>Один проход, три независимых агрегата</h2>
 *
 * <p>Верхний {@link Collectors#teeing} получает каждый элемент потока и передаёт его в две ветки:
 *
 * <ul>
 *   <li>левую — вложенный teeing «топ-3 + доля отмен»;
 *   <li>правую — клиенты с более чем двумя категориями за 30 дней.
 * </ul>
 *
 * <p>{@code teeing} — это не два прохода: JDK создаёт аккумулятор, который принимает элемент и
 * передаёт его обоим downstream-коллекторам. Таким образом, поток читается ровно один раз, а {@code
 * Stream} нельзя переиспользовать — что и требуется.
 *
 * <h2>Топ-3</h2>
 *
 * <p>{@code filtering(!cancelled)} → {@code flatMapping(event -> Stream.of(CategoryProduct))} →
 * {@code groupingBy(category, TreeMap::new, collectingAndThen(toMap(product, revenue, add),
 * top3))}. {@code flatMapping} здесь оставляет место для будущего сценария, когда один заказ
 * содержит несколько позиций; сегодня он разворачивает событие в одну позицию. {@code TreeMap}
 * фиксирует лексикографический порядок категорий. Внутри категории суммы по продукту считаются
 * {@link BigDecimal#add}, а топ сортируется по выручке (обратно) и {@code productId} (вперёд) —
 * ничьи детерминированы.
 *
 * <h2>Доля отмен</h2>
 *
 * <p>{@code teeing(filtering(cancelled, counting()), counting(), rate)}. На пустом потоке {@code
 * total == 0}, и функция возвращает {@code 0.0}, а не {@code NaN}. Считаются все события, включая
 * старые и будущие: это метрика качества всех заказов.
 *
 * <h2>Клиенты</h2>
 *
 * <p>Сначала {@code filtering} оставляет неотменённые события в окне {@code [now - 30d, now]}.
 * Затем {@code groupingBy(customerId, mapping(category, toSet()))} собирает уникальные категории, а
 * {@code collectingAndThen} оставляет клиентов с размером множества {@code > 2} и сортирует id.
 * Отменённый заказ не является покупкой, поэтому в клиентскую активность не попадает.
 *
 * <h2>Сложность</h2>
 *
 * <p>O(N log P + N log C), где P — число уникальных товаров в категориях, C — число клиентов;
 * память O(P + C). Внешних мутабельных переменных нет: всё состояние находится внутри коллекторов.
 */
public final class Kata28_StreamAnalyticsPipelineSolution extends Kata28_StreamAnalyticsPipeline {

  private static final Duration CUSTOMER_WINDOW = Duration.ofDays(30);

  @Override
  public AnalyticsReport analyze(Stream<OrderEvent> events, Instant now) {
    Objects.requireNonNull(events, "events");
    Objects.requireNonNull(now, "now");

    Instant windowStart = now.minus(CUSTOMER_WINDOW);

    Collector<OrderEvent, ?, PartialStats> statsCollector =
        Collectors.teeing(topProductsCollector(), cancellationRateCollector(), PartialStats::new);

    Collector<OrderEvent, ?, AnalyticsReport> reportCollector =
        Collectors.teeing(
            statsCollector,
            customersCollector(windowStart, now),
            (stats, customers) ->
                new AnalyticsReport(
                    stats.topProductsByCategory(), stats.cancelledOrderRate(), customers));

    return events.collect(reportCollector);
  }

  private static Collector<OrderEvent, ?, Map<String, List<ProductRevenue>>>
      topProductsCollector() {
    return Collectors.filtering(
        event -> !event.cancelled(),
        Collectors.flatMapping(
            event ->
                Stream.of(
                    new CategoryProduct(event.category(), event.productId(), event.revenue())),
            Collectors.groupingBy(
                CategoryProduct::category,
                TreeMap::new,
                Collectors.collectingAndThen(
                    Collectors.toMap(
                        CategoryProduct::productId, CategoryProduct::revenue, BigDecimal::add),
                    Kata28_StreamAnalyticsPipelineSolution::topThree))));
  }

  private static Collector<OrderEvent, ?, Double> cancellationRateCollector() {
    return Collectors.teeing(
        Collectors.filtering(OrderEvent::cancelled, Collectors.counting()),
        Collectors.counting(),
        (cancelled, total) -> total == 0L ? 0.0 : (double) cancelled / total);
  }

  private static Collector<OrderEvent, ?, List<String>> customersCollector(
      Instant windowStart, Instant now) {
    return Collectors.filtering(
        event ->
            !event.cancelled()
                && !event.timestamp().isBefore(windowStart)
                && !event.timestamp().isAfter(now),
        Collectors.collectingAndThen(
            Collectors.groupingBy(
                OrderEvent::customerId,
                Collectors.mapping(OrderEvent::category, Collectors.toSet())),
            byCustomer ->
                byCustomer.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 2)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList()));
  }

  private static List<ProductRevenue> topThree(Map<String, BigDecimal> revenueByProduct) {
    return revenueByProduct.entrySet().stream()
        .map(entry -> new ProductRevenue(entry.getKey(), entry.getValue()))
        .sorted(
            Comparator.comparing(ProductRevenue::revenue)
                .reversed()
                .thenComparing(ProductRevenue::productId))
        .limit(3)
        .toList();
  }

  private record PartialStats(
      Map<String, List<ProductRevenue>> topProductsByCategory, double cancelledOrderRate) {}

  private record CategoryProduct(String category, String productId, BigDecimal revenue) {}
}
