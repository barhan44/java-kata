package dev.kata.prod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import dev.kata.prod.Kata28_StreamAnalyticsPipeline.AnalyticsReport;
import dev.kata.prod.Kata28_StreamAnalyticsPipeline.OrderEvent;
import dev.kata.prod.Kata28_StreamAnalyticsPipeline.ProductRevenue;
import dev.kata.solutions.prod.Kata28_StreamAnalyticsPipelineSolution;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 28.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata28* -DuseSolution=true</pre>
 */
@DisplayName("Kata 28: Stream analytics pipeline")
class Kata28_StreamAnalyticsPipelineTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final Instant NOW = Instant.parse("2026-01-15T00:00:00Z");
  private static final Duration DAY = Duration.ofDays(1);

  private Kata28_StreamAnalyticsPipeline newPipeline() {
    return USE_SOLUTION
        ? new Kata28_StreamAnalyticsPipelineSolution()
        : new Kata28_StreamAnalyticsPipeline();
  }

  @Test
  @DisplayName("Топ-3 по категориям с детерминированным tie-break")
  void topThreePerCategory() {
    Stream<OrderEvent> events =
        Stream.of(
            event("o1", "c1", "books", "p1", "100", NOW, false),
            event("o2", "c1", "books", "p2", "200", NOW, false),
            event("o3", "c1", "books", "p3", "300", NOW, false),
            event("o4", "c1", "books", "p4", "400", NOW, false),
            event("o5", "c1", "books", "p5", "50", NOW, false),
            event("o6", "c2", "toys", "t1", "100", NOW, false),
            event("o7", "c2", "toys", "t2", "100", NOW, false),
            event("o8", "c2", "toys", "t3", "50", NOW, false));

    AnalyticsReport report = newPipeline().analyze(events, NOW);

    assertThat(report.topProductsByCategory().keySet()).containsExactly("books", "toys");
    assertThat(report.topProductsByCategory().get("books"))
        .extracting(ProductRevenue::productId)
        .containsExactly("p4", "p3", "p2");
    assertThat(report.topProductsByCategory().get("toys"))
        .extracting(ProductRevenue::productId)
        .containsExactly("t1", "t2", "t3");
    assertThat(report.cancelledOrderRate()).isZero();
  }

  @Test
  @DisplayName("Отменённые заказы не попадают в топ, но учитываются в доле отмен")
  void cancelledOrdersAreExcludedFromTopAndCountedInRate() {
    Stream<OrderEvent> events =
        Stream.of(
            event("o1", "c1", "books", "p1", "100", NOW, false),
            event("o2", "c1", "books", "p2", "1000", NOW, true));

    AnalyticsReport report = newPipeline().analyze(events, NOW);

    assertThat(report.topProductsByCategory().get("books"))
        .extracting(ProductRevenue::productId)
        .containsExactly("p1");
    assertThat(report.cancelledOrderRate()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  @DisplayName("Выручка суммируется по товару, scale BigDecimal не важен")
  void revenueIsSummedPerProduct() {
    Stream<OrderEvent> events =
        Stream.of(
            event("o1", "c1", "books", "p1", "100.0", NOW, false),
            event("o2", "c1", "books", "p1", "100.00", NOW, false),
            event("o3", "c1", "books", "p2", "150", NOW, false));

    AnalyticsReport report = newPipeline().analyze(events, NOW);
    List<ProductRevenue> books = report.topProductsByCategory().get("books");

    assertThat(books).extracting(ProductRevenue::productId).containsExactly("p1", "p2");
    assertThat(books.get(0).revenue()).isEqualByComparingTo("200");
  }

  @Test
  @DisplayName("Пустой поток: доля отмен 0.0, без деления на ноль")
  void emptyStream() {
    AnalyticsReport report = newPipeline().analyze(Stream.empty(), NOW);

    assertThat(report.topProductsByCategory()).isEmpty();
    assertThat(report.cancelledOrderRate()).isZero();
    assertThat(report.customersWithMoreThanTwoCategories()).isEmpty();
  }

  @Test
  @DisplayName("Все заказы отменены: доля 1.0 и пустой топ")
  void allCancelled() {
    AnalyticsReport report =
        newPipeline()
            .analyze(
                Stream.of(
                    event("o1", "c1", "books", "p1", "100", NOW, true),
                    event("o2", "c2", "toys", "t1", "50", NOW, true)),
                NOW);

    assertThat(report.cancelledOrderRate()).isCloseTo(1.0, within(1e-9));
    assertThat(report.topProductsByCategory()).isEmpty();
  }

  @Test
  @DisplayName("Клиенты с более чем 2 категориями за 30 дней; отменённые и будущие не считаются")
  void customersMoreThanTwoCategoriesInWindow() {
    Stream<OrderEvent> events =
        Stream.of(
            // c1: три разные категории в окне -> включён
            event("1", "c1", "A", "p1", "10", NOW.minus(Duration.ofDays(29)), false),
            event("2", "c1", "B", "p2", "10", NOW.minus(DAY), false),
            event("3", "c1", "C", "p3", "10", NOW, false),
            // c2: только две категории -> не включён
            event("4", "c2", "A", "p4", "10", NOW.minus(DAY), false),
            event("5", "c2", "B", "p5", "10", NOW, false),
            // c3: третья категория ровно на границе 30 дней -> включён
            event("6", "c3", "A", "p6", "10", NOW.minus(Duration.ofDays(30)), false),
            event("7", "c3", "B", "p7", "10", NOW.minus(DAY), false),
            event("8", "c3", "C", "p8", "10", NOW, false),
            // c4: третья категория на 1 ns старше окна -> не включён
            event("9", "c4", "A", "p9", "10", NOW.minus(Duration.ofDays(30)).minusNanos(1), false),
            event("10", "c4", "B", "p10", "10", NOW.minus(DAY), false),
            event("11", "c4", "C", "p11", "10", NOW, false),
            // c5: третья категория в отменённом заказе -> не включён
            event("12", "c5", "A", "p12", "10", NOW.minus(DAY), false),
            event("13", "c5", "B", "p13", "10", NOW, false),
            event("14", "c5", "C", "p14", "10", NOW, true),
            // c6: третья категория в будущем -> не включён
            event("15", "c6", "A", "p15", "10", NOW.minus(DAY), false),
            event("16", "c6", "B", "p16", "10", NOW, false),
            event("17", "c6", "C", "p17", "10", NOW.plusSeconds(1), false),
            // c7: повтор одной категории не увеличивает число уникальных -> не включён
            event("18", "c7", "A", "p18", "10", NOW, false),
            event("19", "c7", "A", "p19", "10", NOW, false),
            event("20", "c7", "B", "p20", "10", NOW, false));

    AnalyticsReport report = newPipeline().analyze(events, NOW);

    assertThat(report.customersWithMoreThanTwoCategories()).containsExactly("c1", "c3");
  }

  @Test
  @DisplayName("Топ и доля отмен учитывают события вне 30-дневного окна")
  void topAndRateUseAllEvents() {
    Stream<OrderEvent> events =
        Stream.of(
            event("old", "c1", "books", "old", "1000", NOW.minus(Duration.ofDays(60)), false),
            event("future", "c2", "books", "future", "2000", NOW.plus(Duration.ofDays(1)), false),
            event("cancelled", "c3", "books", "cancelled", "3000", NOW, true));

    AnalyticsReport report = newPipeline().analyze(events, NOW);

    assertThat(report.topProductsByCategory().get("books"))
        .extracting(ProductRevenue::productId)
        .containsExactly("future", "old");
    assertThat(report.cancelledOrderRate()).isCloseTo(1.0 / 3.0, within(1e-9));
    assertThat(report.customersWithMoreThanTwoCategories()).isEmpty();
  }

  @Test
  @DisplayName("Поток потребляется ровно один раз")
  void streamIsConsumedOnce() {
    AtomicInteger seen = new AtomicInteger();
    Stream<OrderEvent> events =
        Stream.of(
                event("o1", "c1", "books", "p1", "100", NOW, false),
                event("o2", "c1", "toys", "p2", "50", NOW, false))
            .peek(event -> seen.incrementAndGet());

    newPipeline().analyze(events, NOW);

    assertThat(seen).hasValue(2);
  }

  @Test
  @DisplayName("Отчёт и его коллекции неизменяемы")
  void reportIsImmutable() {
    AnalyticsReport report =
        newPipeline().analyze(Stream.of(event("o1", "c1", "books", "p1", "100", NOW, false)), NOW);

    assertThatThrownBy(() -> report.topProductsByCategory().put("x", List.of()))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(
            () ->
                report
                    .topProductsByCategory()
                    .get("books")
                    .add(new ProductRevenue("x", BigDecimal.ONE)))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> report.customersWithMoreThanTwoCategories().add("x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация событий, отчёта и null-аргументов")
  void validation() {
    Kata28_StreamAnalyticsPipeline pipeline = newPipeline();
    assertThatThrownBy(() -> pipeline.analyze(null, NOW)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> pipeline.analyze(Stream.of(event("o", "c", "cat", "p", "1", NOW, false)), null))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> new OrderEvent(" ", "c", "cat", "p", BigDecimal.ONE, NOW, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OrderEvent("o", null, "cat", "p", BigDecimal.ONE, NOW, false))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new OrderEvent("o", "c", "cat", "p", null, NOW, false))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ProductRevenue(null, BigDecimal.ONE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new AnalyticsReport(Map.of(), Double.NaN, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new AnalyticsReport(Map.of(), 1.1, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static OrderEvent event(
      String orderId,
      String customerId,
      String category,
      String productId,
      String revenue,
      Instant timestamp,
      boolean cancelled) {
    return new OrderEvent(
        orderId, customerId, category, productId, new BigDecimal(revenue), timestamp, cancelled);
  }
}
