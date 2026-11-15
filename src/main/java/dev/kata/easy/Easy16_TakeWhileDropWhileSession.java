package dev.kata.easy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Easy 16. Сессия ошибки через dropWhile/takeWhile.
 *
 * <h2>Задача</h2>
 *
 * <p>Из журнала извлечь сообщения сессии ошибки: от первого события уровня {@code "ERROR"} до
 * первого последующего {@code "RESOLVED"}. Событие {@code ERROR} включается в результат, {@code
 * RESOLVED} служит правой границей и не включается. Если {@code ERROR} нет — результат пуст. Если
 * {@code RESOLVED} после ошибки нет — берутся все сообщения до конца журнала.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * logs = [INFO("start"), ERROR("db down"), WARN("retry"),
 *         RESOLVED("db up"), ERROR("later")]
 *
 * extractErrorSession → ["db down", "retry"]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code logs.size()};
 *   <li>{@code time} и {@code message} не {@code null}, {@code level} не {@code null}/не blank;
 *   <li>уровни сравниваются без учёта регистра не нужно — ожидаются ровно {@code ERROR} и {@code
 *       RESOLVED} в верхнем регистре.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(k), где k — длина сессии. Эталон использует {@code dropWhile} до первой
 * ошибки и {@code takeWhile} до первого RESOLVED; промежуточные списки не создаются. Результат
 * неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy16_TakeWhileDropWhileSessionSolution}.
 */
public class Easy16_TakeWhileDropWhileSession {

  /** Строка журнала. */
  public record LogLine(Instant time, String level, String message) {
    public LogLine {
      Objects.requireNonNull(time, "time");
      Objects.requireNonNull(level, "level");
      if (level.isBlank()) {
        throw new IllegalArgumentException("level must not be blank");
      }
      Objects.requireNonNull(message, "message");
    }
  }

  /** Возвращает сообщения первой сессии ERROR до RESOLVED (не включая RESOLVED). */
  public List<String> extractErrorSession(List<LogLine> logs) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
