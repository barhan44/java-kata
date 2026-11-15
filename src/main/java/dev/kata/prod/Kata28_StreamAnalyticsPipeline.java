package dev.kata.prod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Kata 28. Аналитический pipeline на Stream API: топ-3, доля отмен и активные клиенты.
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Маркетинг хочет один отчёт по потоку заказов: самые доходные товары в каждой категории,
 * процент отменённых заказов и клиентов, которые за последние 30 дней покупали более чем в двух
 * категориях. Поток может быть большим (или бесконечным), поэтому нельзя материализовать его в
 * список и делать несколько проходов. Нужен один терминальный {@code collect} с чистыми
 * коллекторами, без мутабельных внешних переменных и без деления на ноль на пустом потоке.
 *
 * <h2>Правила отчёта</h2>
 *
 * <ol>
 *   <li><b>Топ-3 товара в категории</b> — по суммарной выручке неотменённых заказов. Сортировка:
 *       выручка по убыванию, при равенстве — {@code productId} по возрастанию (детерминированные
 *       ничьи). В категории может быть меньше трёх товаров.
 *   <li><b>Доля отменённых заказов</b> — {@code cancelled / total} по всем событиям, включая
 *       старые/будущие. Пустой поток — {@code 0.0}, без {@code NaN}.
 *   <li><b>Клиенты с >2 категориями за 30 дней</b> — учитываются только неотменённые заказы с
 *       timestamp в {@code [now - 30 days, now]} (обе границы включительно). Категории считаются
 *       уникальными. Результат отсортирован по {@code customerId}.
 *   <li>Топ и клиенты не учитывают отменённые заказы; доля отмен — учитывает все.
 * </ol>
 *
 * <h2>Один проход</h2>
 *
 * <p>Используется вложенный {@link java.util.stream.Collectors#teeing}: верхний teeing объединяет
 * статистику (топ + доля отмен) и клиентов, внутренний — топ и долю отмен. Каждый элемент потока
 * проходит через {@code filtering}/{@code flatMapping} к нужным листьям, но сам поток потребляется
 * ровно один раз. Коллекторы ({@code groupingBy}, {@code collectingAndThen}, {@code filtering},
 * {@code flatMapping}) создают внутреннее состояние сами; внешних мутабельных переменных нет.
 *
 * <h2>Сложность</h2>
 *
 * <ul>
 *   <li>Время — O(N log P + N log C), где P — товары в категориях, C — клиенты; в типичном случае
 *       близко к O(N).
 *   <li>Память — O(P + C) для агрегатов и O(1) дополнительно на событие.
 * </ul>
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>{@code null} stream/now/событие — {@link NullPointerException}.
 *   <li>Пустой поток — пустая карта, доля отмен {@code 0.0}, пустой список клиентов.
 *   <li>Равная выручка — tie-break по productId; порядок категорий — лексикографический.
 *   <li>Границы 30-дневного окна включительны; будущие события не учитываются.
 *   <li>Отрицательная выручка допустима (возвраты), отменённые заказы в топ не попадают.
 * </ul>
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.prod.Kata28_StreamAnalyticsPipelineSolution}.
 * Тесты: {@code Kata28_StreamAnalyticsPipelineTest}.
 */
public class Kata28_StreamAnalyticsPipeline {

  /** Событие заказа. */
  public record OrderEvent(
      String orderId,
      String customerId,
      String category,
      String productId,
      BigDecimal revenue,
      Instant timestamp,
      boolean cancelled) {

    public OrderEvent {
      requireText(orderId, "orderId");
      requireText(customerId, "customerId");
      requireText(category, "category");
      requireText(productId, "productId");
      Objects.requireNonNull(revenue, "revenue");
      Objects.requireNonNull(timestamp, "timestamp");
    }

    private static void requireText(String value, String field) {
      Objects.requireNonNull(value, field);
      if (value.isBlank()) {
        throw new IllegalArgumentException(field + " must not be blank");
      }
    }
  }

  /** Выручка товара в категории. */
  public record ProductRevenue(String productId, BigDecimal revenue) {
    public ProductRevenue {
      Objects.requireNonNull(productId, "productId");
      Objects.requireNonNull(revenue, "revenue");
    }
  }

  /** Итоговый отчёт. */
  public record AnalyticsReport(
      Map<String, List<ProductRevenue>> topProductsByCategory,
      double cancelledOrderRate,
      List<String> customersWithMoreThanTwoCategories) {

    public AnalyticsReport {
      Objects.requireNonNull(topProductsByCategory, "topProductsByCategory");
      Objects.requireNonNull(
          customersWithMoreThanTwoCategories, "customersWithMoreThanTwoCategories");
      if (!(cancelledOrderRate >= 0.0 && cancelledOrderRate <= 1.0)) {
        throw new IllegalArgumentException(
            "cancelledOrderRate must be in [0, 1], got " + cancelledOrderRate);
      }

      Map<String, List<ProductRevenue>> copy = new TreeMap<>();
      for (Map.Entry<String, List<ProductRevenue>> entry : topProductsByCategory.entrySet()) {
        copy.put(
            Objects.requireNonNull(entry.getKey(), "category"),
            List.copyOf(Objects.requireNonNull(entry.getValue(), "products")));
      }
      topProductsByCategory = Collections.unmodifiableMap(copy);
      customersWithMoreThanTwoCategories = List.copyOf(customersWithMoreThanTwoCategories);
    }
  }

  /**
   * Строит отчёт за один проход по потоку.
   *
   * @param events поток событий заказов
   * @param now опорное время для 30-дневного окна
   * @return аналитический отчёт
   */
  public AnalyticsReport analyze(Stream<OrderEvent> events, Instant now) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
