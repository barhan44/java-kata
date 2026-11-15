package dev.kata.solutions.easy;

import dev.kata.easy.Easy32_DurationHumanizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Эталон Easy 32.
 *
 * <p>Компоненты берутся методами {@code to*Part()}: {@code toDaysPart()} — целые дни, {@code
 * toHoursPart()} — часы внутри дня (0..23), минуты и секунды аналогично. Нулевые части не
 * добавляются, поэтому {@code Duration.ofHours(24)} даёт {@code "1d"}, а не {@code "0d 24h"}.
 *
 * <p>Для отрицательных длительностей знак снимается через {@code abs()}, форматирование выполняется
 * для модуля, затем к непустому результату добавляется {@code "-"}. Субсекундная длительность даёт
 * {@code "0s"} — формат намеренно ограничен секундами.
 */
public final class Easy32_DurationHumanizerSolution extends Easy32_DurationHumanizer {

  @Override
  public String formatDuration(Duration duration) {
    Objects.requireNonNull(duration, "duration");

    Duration absolute = duration.isNegative() ? duration.abs() : duration;
    List<String> parts = new ArrayList<>();

    long days = absolute.toDaysPart();
    if (days != 0) {
      parts.add(days + "d");
    }
    int hours = absolute.toHoursPart();
    if (hours != 0) {
      parts.add(hours + "h");
    }
    int minutes = absolute.toMinutesPart();
    if (minutes != 0) {
      parts.add(minutes + "m");
    }
    int seconds = absolute.toSecondsPart();
    if (seconds != 0) {
      parts.add(seconds + "s");
    }

    String formatted = parts.isEmpty() ? "0s" : String.join(" ", parts);
    return duration.isNegative() && !formatted.equals("0s") ? "-" + formatted : formatted;
  }
}
