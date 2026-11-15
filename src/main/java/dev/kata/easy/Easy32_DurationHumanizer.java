package dev.kata.easy;

import java.time.Duration;

/**
 * Easy 32. Человекочитаемая длительность.
 *
 * <h2>Задача</h2>
 *
 * <p>Преобразовать {@link Duration} в строку из ненулевых частей: дни, часы, минуты, секунды.
 * Нулевая длительность — {@code "0s"}. Формат частей: {@code 2d 4h 15m 30s}, разделитель — пробел.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * formatDuration(Duration.ZERO)                         → "0s"
 * formatDuration(Duration.ofSeconds(90))                → "1m 30s"
 * formatDuration(Duration.ofHours(4).plusMinutes(5))    → "4h 5m"
 * formatDuration(Duration.ofDays(2).plusHours(4)
 *     .plusMinutes(15).plusSeconds(30))                 → "2d 4h 15m 30s"
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>{@code duration} не {@code null};
 *   <li>точность — секунды; доли секунды отбрасываются.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(1), память O(1). Эталон использует {@code toDaysPart()}, {@code toHoursPart()}, {@code
 * toMinutesPart()} и {@code toSecondsPart()} — они уже разбивают Duration на компоненты без ручной
 * арифметики. Отрицательная длительность форматируется со знаком минус.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy32_DurationHumanizerSolution}.
 */
public class Easy32_DurationHumanizer {

  /** Форматирует длительность в стиле {@code "2d 4h 15m 30s"}. */
  public String formatDuration(Duration duration) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
