package dev.kata.easy;

import java.time.LocalDate;
import java.util.Set;

/**
 * Easy 30. Прибавление рабочих дней (Stream.iterate).
 *
 * <h2>Задача</h2>
 *
 * <p>Прибавить к дате {@code businessDaysToAdd} рабочих дней, пропуская субботы, воскресенья и
 * праздники из {@code publicHolidays}. Положительное число двигает вперёд, отрицательное — назад.
 * Отсчёт начинается со следующего дня после {@code startDate}; {@code 0} возвращает исходную дату
 * без изменений, даже если она выходная или праздничная.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * start = Friday 2026-01-02, holidays = []
 * addBusinessDays(start, 1) → Monday 2026-01-05
 *
 * start = Monday 2026-01-05, holidays = {2026-01-06}
 * addBusinessDays(start, 2) → Wednesday 2026-01-07
 *
 * addBusinessDays(anyDate, 0) → anyDate
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>{@code startDate} и {@code publicHolidays} не {@code null};
 *   <li>{@code businessDaysToAdd} — любое {@code int}; знак задаёт направление.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n + h) в худшем случае, где n — число добавляемых дней, h — размер множества
 * праздников; память O(1) благодаря ленивому {@link java.util.stream.Stream#iterate}. Каждый шаг —
 * {@code plusDays(direction)}; фильтр оставляет рабочие дни, {@code limit} останавливает поток на
 * нужном количестве.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy30_BusinessDaysCalculatorSolution}.
 */
public class Easy30_BusinessDaysCalculator {

  /** Прибавляет рабочие дни с пропуском выходных и праздников. */
  public LocalDate addBusinessDays(
      LocalDate startDate, int businessDaysToAdd, Set<LocalDate> publicHolidays) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
