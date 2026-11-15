package dev.kata.easy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Easy 31. Поиск пересечения рабочих часов в таймзонах.
 *
 * <h2>Задача</h2>
 *
 * <p>Для каждого участника задано рабочее окно {@code [workStart, workEnd)} в его таймзоне. Найти
 * все целые часы даты {@code utcDate} в UTC, когда локальное время каждого участника попадает в его
 * окно.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * participants = [
 *   Participant("Berlin", Europe/Berlin, 09:00, 17:00),
 *   Participant("NY",     America/New_York, 09:00, 17:00)
 * ]
 * utcDate = 2026-01-15
 *
 * findAvailableUtcHours → [14:00Z, 15:00Z]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code participants.size()}; пустой список означает «все 24 часа доступны»;
 *   <li>{@code name} не blank, {@code zone/workStart/workEnd} не {@code null}, {@code workStart <
 *       workEnd}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(24 × P), память O(24). Эталон перебирает часы через {@code IntStream.range(0, 24)},
 * переводит кандидата в таймзону участника через {@code withZoneSameInstant} и проверяет окно
 * {@code [start, end)}. Такой подход учитывает смещения и переход на летнее время.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy31_TimeZoneMeetingFinderSolution}.
 */
public class Easy31_TimeZoneMeetingFinder {

  /** Участник и его рабочее окно. */
  public record Participant(String name, ZoneId zone, LocalTime workStart, LocalTime workEnd) {
    public Participant {
      Objects.requireNonNull(name, "name");
      if (name.isBlank()) {
        throw new IllegalArgumentException("name must not be blank");
      }
      Objects.requireNonNull(zone, "zone");
      Objects.requireNonNull(workStart, "workStart");
      Objects.requireNonNull(workEnd, "workEnd");
      if (!workStart.isBefore(workEnd)) {
        throw new IllegalArgumentException("workStart must be before workEnd");
      }
    }
  }

  /** Возвращает UTC-часы, доступные всем участникам. */
  public List<ZonedDateTime> findAvailableUtcHours(
      List<Participant> participants, LocalDate utcDate) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
