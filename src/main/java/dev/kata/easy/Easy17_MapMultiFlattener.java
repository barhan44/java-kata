package dev.kata.easy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Easy 17. mapMulti + Pattern Matching for switch.
 *
 * <h2>Задача</h2>
 *
 * <p>Дан поток событий трёх типов. Развернуть их в плоский список аудит-строк через {@code
 * Stream.mapMulti} и {@code switch} с pattern matching. Одно событие может дать несколько строк.
 *
 * <h2>Формат строк</h2>
 *
 * <ul>
 *   <li>{@code LoginEvent} → {@code "LOGIN <user> @<time>"};
 *   <li>{@code PurchaseEvent} → {@code "PURCHASE <user> <amount> @<time>"};
 *   <li>{@code ErrorEvent} → две строки: {@code "ERROR <code> @<time>"} и {@code "MESSAGE
 *       <message>"}.
 * </ul>
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * events = [LoginEvent("alice", t), ErrorEvent("E500", "boom", t)]
 *
 * auditLines → ["LOGIN alice @t", "ERROR E500 @t", "MESSAGE boom"]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code events.size()};
 *   <li>строковые поля не {@code null}/не blank, {@code time} и {@code amount} не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n + k), память O(k), где k — число аудит-строк. {@code mapMulti} позволяет одному
 * событию отдать 0..m строк без создания промежуточных коллекций; исчерпывающий {@code switch} по
 * sealed-иерархии проверяется компилятором. Результат неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy17_MapMultiFlattenerSolution}.
 */
public class Easy17_MapMultiFlattener {

  /** Событие; иерархия закрыта для расширения. */
  public sealed interface Event permits LoginEvent, PurchaseEvent, ErrorEvent {}

  /** Вход пользователя. */
  public record LoginEvent(String user, Instant time) implements Event {
    public LoginEvent {
      requireText(user, "user");
      Objects.requireNonNull(time, "time");
    }
  }

  /** Покупка. */
  public record PurchaseEvent(String user, BigDecimal amount, Instant time) implements Event {
    public PurchaseEvent {
      requireText(user, "user");
      Objects.requireNonNull(amount, "amount");
      Objects.requireNonNull(time, "time");
    }
  }

  /** Ошибка. */
  public record ErrorEvent(String code, String message, Instant time) implements Event {
    public ErrorEvent {
      requireText(code, "code");
      requireText(message, "message");
      Objects.requireNonNull(time, "time");
    }
  }

  /** Разворачивает события в аудит-строки. */
  public List<String> auditLines(List<Event> events) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
