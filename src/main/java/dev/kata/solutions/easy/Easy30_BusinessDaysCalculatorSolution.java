package dev.kata.solutions.easy;

import dev.kata.easy.Easy30_BusinessDaysCalculator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Эталон Easy 30.
 *
 * <p>{@code Stream.iterate(seed, next)} создаёт бесконечную последовательность дат в нужном
 * направлении. {@code filter} оставляет только рабочие дни (не суббота/воскресенье и не праздник),
 * {@code limit(abs(n))} останавливает поток, а {@code reduce((first, second) -> second)} берёт
 * последний элемент — то есть n-й рабочий день. Для {@code n == 0} поток не нужен: возвращается
 * исходная дата.
 *
 * <p>{@code Math.abs((long) businessDaysToAdd)} защищает от переполнения при {@code
 * Integer.MIN_VALUE}. Расчёт не зависит от часового пояса — используются только даты.
 */
public final class Easy30_BusinessDaysCalculatorSolution extends Easy30_BusinessDaysCalculator {

  @Override
  public LocalDate addBusinessDays(
      LocalDate startDate, int businessDaysToAdd, Set<LocalDate> publicHolidays) {
    Objects.requireNonNull(startDate, "startDate");
    Objects.requireNonNull(publicHolidays, "publicHolidays");
    if (businessDaysToAdd == 0) {
      return startDate;
    }

    int step = businessDaysToAdd > 0 ? 1 : -1;
    long count = Math.abs((long) businessDaysToAdd);

    return Stream.iterate(startDate.plusDays(step), date -> date.plusDays(step))
        .filter(date -> isBusinessDay(date, publicHolidays))
        .limit(count)
        .reduce((first, second) -> second)
        .orElse(startDate);
  }

  private static boolean isBusinessDay(LocalDate date, Set<LocalDate> publicHolidays) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !publicHolidays.contains(date);
  }
}
