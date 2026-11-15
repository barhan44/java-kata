package dev.kata.prod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.prod.Kata26_LedgerReconciliation.AmountMismatch;
import dev.kata.prod.Kata26_LedgerReconciliation.ReconciliationReport;
import dev.kata.prod.Kata26_LedgerReconciliation.Status;
import dev.kata.prod.Kata26_LedgerReconciliation.Transaction;
import dev.kata.solutions.prod.Kata26_LedgerReconciliationSolution;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 26.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata26* -DuseSolution=true</pre>
 */
@DisplayName("Kata 26: сверка транзакций")
class Kata26_LedgerReconciliationTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
  private static final Currency USD = Currency.getInstance("USD");
  private static final Currency EUR = Currency.getInstance("EUR");
  private static final Duration DRIFT = Duration.ofSeconds(5);
  private static final Duration GRACE = Duration.ofSeconds(10);

  private Kata26_LedgerReconciliation newReconciler() {
    return USE_SOLUTION
        ? new Kata26_LedgerReconciliationSolution()
        : new Kata26_LedgerReconciliation();
  }

  private ReconciliationReport reconcile(List<Transaction> internal, List<Transaction> gateway) {
    return newReconciler().reconcile(internal, gateway, DRIFT, GRACE, NOW);
  }

  @Test
  @DisplayName("BigDecimal 10.0 и 10.00 совпадают; drift на границе включителен")
  void matchedWithScaleAndDriftBoundary() {
    Transaction internal = tx("t1", "10.0", USD, NOW);
    Transaction gateway = tx("t1", "10.00", USD, NOW.plus(DRIFT));

    ReconciliationReport report =
        reconcile(
            List.of(internal, tx("t2", "20", USD, NOW)),
            List.of(gateway, tx("t2", "20.000", USD, NOW)));

    assertThat(report.matched()).hasSize(2);
    assertThat(report.matched())
        .extracting(match -> match.internal().id())
        .containsExactly("t1", "t2");
    assertThat(report.amountMismatch()).isEmpty();
    assertThat(report.missingInGateway()).isEmpty();
    assertThat(report.missingInInternal()).isEmpty();
    assertThat(report.isReconciled()).isTrue();
  }

  @Test
  @DisplayName("Расхождения суммы, валюты и времени попадают в amountMismatch")
  void amountCurrencyAndTimeMismatches() {
    Transaction amountInternal = tx("amount", "10.00", USD, NOW);
    Transaction amountGateway = tx("amount", "10.01", USD, NOW);

    Transaction currencyInternal = tx("currency", "10.00", USD, NOW);
    Transaction currencyGateway = tx("currency", "10.00", EUR, NOW);

    Transaction timeInternal = tx("time", "10.00", USD, NOW);
    Transaction timeGateway = tx("time", "10.00", USD, NOW.plus(DRIFT).plusNanos(1));

    ReconciliationReport report =
        reconcile(
            List.of(amountInternal, currencyInternal, timeInternal),
            List.of(amountGateway, currencyGateway, timeGateway));

    assertThat(report.matched()).isEmpty();
    assertThat(report.amountMismatch())
        .extracting(mismatch -> mismatch.internal().id())
        .containsExactly("amount", "currency", "time");
    assertThat(report.amountMismatch())
        .allSatisfy(mismatch -> assertThat(mismatch.gateway()).isNotNull());
    assertThat(report.isReconciled()).isFalse();
  }

  @Test
  @DisplayName("Grace period: ровно на границе — missing, моложе — тихо пропускается")
  void gracePeriodBoundary() {
    Transaction atBoundary = tx("boundary", "10", USD, NOW.minus(GRACE));
    Transaction justYounger = tx("younger", "10", USD, NOW.minus(GRACE).plusNanos(1));
    Transaction veryFresh = tx("fresh", "10", USD, NOW.minusSeconds(1));
    Transaction fromFuture = tx("future", "10", USD, NOW.plusSeconds(1));

    ReconciliationReport report =
        reconcile(List.of(atBoundary, justYounger, veryFresh, fromFuture), List.of());

    assertThat(report.missingInGateway()).extracting(Transaction::id).containsExactly("boundary");
    assertThat(report.matched()).isEmpty();
    assertThat(report.amountMismatch()).isEmpty();
    assertThat(report.missingInInternal()).isEmpty();
    assertThat(report.isReconciled()).isFalse();
  }

  @Test
  @DisplayName("Транзакция только в шлюзе попадает в missingInInternal в порядке шлюза")
  void missingInInternal() {
    Transaction first = tx("g1", "1", USD, NOW);
    Transaction second = tx("g2", "2", USD, NOW);

    ReconciliationReport report = reconcile(List.of(), List.of(first, second));

    assertThat(report.missingInInternal()).extracting(Transaction::id).containsExactly("g1", "g2");
    assertThat(report.missingInGateway()).isEmpty();
    assertThat(report.matched()).isEmpty();
    assertThat(report.isReconciled()).isFalse();
  }

  @Test
  @DisplayName("Пустые списки дают пустой сбалансированный отчёт")
  void emptyLists() {
    ReconciliationReport report = reconcile(List.of(), List.of());

    assertThat(report.matched()).isEmpty();
    assertThat(report.amountMismatch()).isEmpty();
    assertThat(report.missingInGateway()).isEmpty();
    assertThat(report.missingInInternal()).isEmpty();
    assertThat(report.isReconciled()).isTrue();
  }

  @Test
  @DisplayName("Статус не участвует в сопоставлении")
  void statusIsIgnored() {
    Transaction internal = new Transaction("t1", new BigDecimal("10"), USD, NOW, Status.COMPLETED);
    Transaction gateway =
        new Transaction("t1", new BigDecimal("10.00"), USD, NOW, Status.CANCELLED);

    ReconciliationReport report = reconcile(List.of(internal), List.of(gateway));

    assertThat(report.matched()).hasSize(1);
    assertThat(report.isReconciled()).isTrue();
  }

  @Test
  @DisplayName("Порядок внутреннего списка сохраняется в категориях отчёта")
  void preservesOrder() {
    Transaction matchedInternal = tx("m", "1", USD, NOW);
    Transaction missingInternal = tx("missing", "2", USD, NOW.minusSeconds(60));
    Transaction mismatchInternal = tx("x", "3", USD, NOW);

    ReconciliationReport report =
        reconcile(
            List.of(matchedInternal, missingInternal, mismatchInternal),
            List.of(
                tx("x", "4", USD, NOW),
                tx("m", "1.00", USD, NOW),
                tx("gateway-only", "5", USD, NOW)));

    assertThat(report.matched()).extracting(match -> match.internal().id()).containsExactly("m");
    assertThat(report.missingInGateway()).extracting(Transaction::id).containsExactly("missing");
    assertThat(report.amountMismatch())
        .extracting(mismatch -> mismatch.internal().id())
        .containsExactly("x");
    assertThat(report.missingInInternal())
        .extracting(Transaction::id)
        .containsExactly("gateway-only");
  }

  @Test
  @DisplayName("Только свежие внутренние транзакции не делают отчёт несбалансированным")
  void freshTransactionsAreIntentionallyOmitted() {
    ReconciliationReport report =
        reconcile(List.of(tx("fresh", "1", USD, NOW.minusSeconds(1))), List.of());

    assertThat(report.matched()).isEmpty();
    assertThat(report.missingInGateway()).isEmpty();
    assertThat(report.isReconciled()).isTrue();
  }

  @Test
  @DisplayName("Дубликаты id в любом списке отвергаются")
  void duplicateIdsRejected() {
    Transaction first = tx("dup", "1", USD, NOW);
    Transaction second = tx("dup", "1.00", USD, NOW);

    assertThatThrownBy(() -> reconcile(List.of(first, second), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dup");
    assertThatThrownBy(() -> reconcile(List.of(), List.of(first, second)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dup");
  }

  @Test
  @DisplayName("null-аргументы и отрицательные Duration отвергаются")
  void invalidArguments() {
    Kata26_LedgerReconciliation reconciler = newReconciler();
    Transaction transaction = tx("t", "1", USD, NOW);

    assertThatThrownBy(() -> reconciler.reconcile(null, List.of(), DRIFT, GRACE, NOW))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> reconciler.reconcile(List.of(), null, DRIFT, GRACE, NOW))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> reconciler.reconcile(List.of(), List.of(), null, GRACE, NOW))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> reconciler.reconcile(List.of(), List.of(), DRIFT, null, NOW))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> reconciler.reconcile(List.of(), List.of(), DRIFT, GRACE, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> reconciler.reconcile(List.of(), List.of(), Duration.ofMillis(-1), GRACE, NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> reconciler.reconcile(List.of(), List.of(), DRIFT, Duration.ofMillis(-1), NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                reconciler.reconcile(
                    java.util.Arrays.asList((Transaction) null), List.of(), DRIFT, GRACE, NOW))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                reconciler.reconcile(
                    List.of(transaction),
                    java.util.Arrays.asList((Transaction) null),
                    DRIFT,
                    GRACE,
                    NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Списки в отчёте неизменяемы")
  void reportListsAreImmutable() {
    Transaction transaction = tx("t", "1", USD, NOW);
    ReconciliationReport report = reconcile(List.of(transaction), List.of(transaction));

    assertThatThrownBy(() -> report.matched().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> report.amountMismatch().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> report.missingInGateway().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> report.missingInInternal().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация Transaction: пустой id и null-поля запрещены")
  void transactionValidation() {
    assertThatThrownBy(() -> new Transaction(" ", BigDecimal.ONE, USD, NOW, Status.COMPLETED))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Transaction(null, BigDecimal.ONE, USD, NOW, Status.COMPLETED))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Transaction("id", null, USD, NOW, Status.COMPLETED))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Transaction("id", BigDecimal.ONE, null, NOW, Status.COMPLETED))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Transaction("id", BigDecimal.ONE, USD, null, Status.COMPLETED))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Transaction("id", BigDecimal.ONE, USD, NOW, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("AmountMismatch содержит обе транзакции")
  void mismatchContainsBothTransactions() {
    Transaction internal = tx("t", "10", USD, NOW);
    Transaction gateway = tx("t", "11", USD, NOW);

    ReconciliationReport report = reconcile(List.of(internal), List.of(gateway));
    AmountMismatch mismatch = report.amountMismatch().get(0);

    assertThat(mismatch.internal()).isSameAs(internal);
    assertThat(mismatch.gateway()).isSameAs(gateway);
  }

  private static Transaction tx(String id, String amount, Currency currency, Instant timestamp) {
    return new Transaction(id, new BigDecimal(amount), currency, timestamp, Status.COMPLETED);
  }
}
