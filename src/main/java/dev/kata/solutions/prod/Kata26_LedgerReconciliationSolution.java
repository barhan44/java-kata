package dev.kata.solutions.prod;

import dev.kata.prod.Kata26_LedgerReconciliation;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Эталонное решение Kata 26.
 *
 * <h2>Один проход и индексы</h2>
 *
 * <p>Сначала строится {@code Map<id, Transaction>} по шлюзу (O(M)) с проверкой дубликатов. Затем
 * один проход по внутреннему списку (O(N)): для каждого id сразу видно, есть ли пара в шлюзе.
 * Второй проход по шлюзу добавляет {@code missingInInternal}; его можно было бы сделать и в первом
 * проходе, но отдельный проход сохраняет порядок шлюза и не требует второго множества. Итог — O(N +
 * M), память O(N + M).
 *
 * <h2>Почему compareTo, а не equals</h2>
 *
 * <p>{@link java.math.BigDecimal#equals} учитывает scale: {@code 10.0} и {@code 10.00} — разные
 * значения. Для денег важна только числовая величина, поэтому используется {@code compareTo == 0}.
 * Это классическая ловушка, из-за которой сверка может показать ложное расхождение.
 *
 * <h2>Время и grace</h2>
 *
 * <p>Разница timestamps считается через {@link Duration#between}; модуль сравнивается с {@code
 * maxTimeDrift} включительно. Свежесть внутренней транзакции: {@code Duration.between(tx, now) <
 * gracePeriod}. Если timestamp в будущем (часы разъехались), разница отрицательна и транзакция
 * считается свежей — её не объявляем потерянной. Ровно на границе grace она уже не свежая и
 * попадает в {@code missingInGateway}.
 *
 * <h2>Порядок и категории</h2>
 *
 * <p>Результаты первых трёх категорий идут в порядке внутреннего списка, {@code missingInInternal}
 * — в порядке шлюза. Статус намеренно не участвует в сравнении: это информационное поле, а не
 * финансовая величина. Пары с одинаковой суммой и валютой, но разницей времени больше допуска,
 * попадают в {@code amountMismatch}, потому что в отчёте только четыре категории.
 */
public final class Kata26_LedgerReconciliationSolution extends Kata26_LedgerReconciliation {

  @Override
  public ReconciliationReport reconcile(
      List<Transaction> internal,
      List<Transaction> gateway,
      Duration maxTimeDrift,
      Duration gracePeriod,
      Instant now) {
    Objects.requireNonNull(internal, "internal");
    Objects.requireNonNull(gateway, "gateway");
    Objects.requireNonNull(maxTimeDrift, "maxTimeDrift");
    Objects.requireNonNull(gracePeriod, "gracePeriod");
    Objects.requireNonNull(now, "now");
    if (maxTimeDrift.isNegative()) {
      throw new IllegalArgumentException("maxTimeDrift must be >= 0, got " + maxTimeDrift);
    }
    if (gracePeriod.isNegative()) {
      throw new IllegalArgumentException("gracePeriod must be >= 0, got " + gracePeriod);
    }

    Map<String, Transaction> gatewayById = new HashMap<>();
    for (Transaction transaction : gateway) {
      Objects.requireNonNull(transaction, "gateway transaction");
      if (gatewayById.putIfAbsent(transaction.id(), transaction) != null) {
        throw new IllegalArgumentException(
            "duplicate transaction id in gateway: " + transaction.id());
      }
    }

    Set<String> internalIds = new HashSet<>();
    List<Match> matched = new ArrayList<>();
    List<AmountMismatch> amountMismatch = new ArrayList<>();
    List<Transaction> missingInGateway = new ArrayList<>();

    for (Transaction transaction : internal) {
      Objects.requireNonNull(transaction, "internal transaction");
      if (!internalIds.add(transaction.id())) {
        throw new IllegalArgumentException(
            "duplicate transaction id in internal ledger: " + transaction.id());
      }

      Transaction gatewayTransaction = gatewayById.get(transaction.id());
      if (gatewayTransaction == null) {
        if (!isFresh(transaction.timestamp(), now, gracePeriod)) {
          missingInGateway.add(transaction);
        }
      } else if (matches(transaction, gatewayTransaction, maxTimeDrift)) {
        matched.add(new Match(transaction, gatewayTransaction));
      } else {
        amountMismatch.add(new AmountMismatch(transaction, gatewayTransaction));
      }
    }

    List<Transaction> missingInInternal = new ArrayList<>();
    for (Transaction transaction : gateway) {
      if (!internalIds.contains(transaction.id())) {
        missingInInternal.add(transaction);
      }
    }

    return new ReconciliationReport(matched, amountMismatch, missingInGateway, missingInInternal);
  }

  private static boolean matches(Transaction internal, Transaction gateway, Duration maxTimeDrift) {
    if (internal.amount().compareTo(gateway.amount()) != 0) {
      return false;
    }
    if (!internal.currency().equals(gateway.currency())) {
      return false;
    }
    Duration drift = Duration.between(internal.timestamp(), gateway.timestamp()).abs();
    return drift.compareTo(maxTimeDrift) <= 0;
  }

  private static boolean isFresh(Instant timestamp, Instant now, Duration gracePeriod) {
    return Duration.between(timestamp, now).compareTo(gracePeriod) < 0;
  }
}
