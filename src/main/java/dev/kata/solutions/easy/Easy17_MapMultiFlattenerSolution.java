package dev.kata.solutions.easy;

import dev.kata.easy.Easy17_MapMultiFlattener;
import java.util.List;
import java.util.Objects;

/**
 * Эталон Easy 17.
 *
 * <p>{@code mapMulti} принимает {@code BiConsumer<Event, Consumer<String>>}: в зависимости от типа
 * события можно вызвать {@code downstream.accept} один или несколько раз. Для {@code ErrorEvent}
 * выдаются две строки — именно тот сценарий, ради которого mapMulti удобнее {@code map}/{@code
 * flatMap}.
 *
 * <p>{@code switch} без {@code default} исчерпывающ: sealed-интерфейс перечисляет все реализации, и
 * компилятор не даст забыть новую. Явный type witness {@code <String>} фиксирует тип результата.
 */
public final class Easy17_MapMultiFlattenerSolution extends Easy17_MapMultiFlattener {

  @Override
  public List<String> auditLines(List<Event> events) {
    Objects.requireNonNull(events, "events");

    return events.stream()
        .<String>mapMulti(
            (event, downstream) -> {
              switch (event) {
                case LoginEvent login ->
                    downstream.accept("LOGIN " + login.user() + " @" + login.time());
                case PurchaseEvent purchase ->
                    downstream.accept(
                        "PURCHASE "
                            + purchase.user()
                            + " "
                            + purchase.amount().toPlainString()
                            + " @"
                            + purchase.time());
                case ErrorEvent error -> {
                  downstream.accept("ERROR " + error.code() + " @" + error.time());
                  downstream.accept("MESSAGE " + error.message());
                }
              }
            })
        .toList();
  }
}
