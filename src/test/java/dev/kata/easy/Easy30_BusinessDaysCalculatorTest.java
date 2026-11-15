package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy30_BusinessDaysCalculatorSolution;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 30: Business days calculator")
class Easy30_BusinessDaysCalculatorTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final LocalDate FRIDAY = LocalDate.of(2026, 1, 2);
  private static final LocalDate SATURDAY = LocalDate.of(2026, 1, 3);
  private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);
  private static final LocalDate WEDNESDAY = LocalDate.of(2026, 1, 7);

  private Easy30_BusinessDaysCalculator newSolution() {
    return USE_SOLUTION
        ? new Easy30_BusinessDaysCalculatorSolution()
        : new Easy30_BusinessDaysCalculator();
  }

  @Test
  @DisplayName("Пятница + 1 рабочий день = понедельник")
  void skipsWeekend() {
    assertThat(newSolution().addBusinessDays(FRIDAY, 1, Set.of())).isEqualTo(MONDAY);
  }

  @Test
  @DisplayName("Старт в субботу: следующий рабочий день — понедельник")
  void startsOnWeekend() {
    assertThat(newSolution().addBusinessDays(SATURDAY, 1, Set.of())).isEqualTo(MONDAY);
  }

  @Test
  @DisplayName("Праздник пропускается")
  void skipsHoliday() {
    assertThat(newSolution().addBusinessDays(FRIDAY, 2, Set.of(MONDAY))).isEqualTo(WEDNESDAY);
    assertThat(newSolution().addBusinessDays(FRIDAY, 2, Set.of(LocalDate.of(2026, 1, 6))))
        .isEqualTo(LocalDate.of(2026, 1, 7));
  }

  @Test
  @DisplayName("Несколько дней с переходом через выходные")
  void multipleDays() {
    assertThat(newSolution().addBusinessDays(WEDNESDAY, 3, Set.of()))
        .isEqualTo(LocalDate.of(2026, 1, 12));
  }

  @Test
  @DisplayName("Отрицательное число двигает назад")
  void negativeDays() {
    assertThat(newSolution().addBusinessDays(MONDAY, -1, Set.of())).isEqualTo(FRIDAY);
    assertThat(newSolution().addBusinessDays(MONDAY, -2, Set.of()))
        .isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  @DisplayName("Ноль возвращает исходную дату даже в выходной")
  void zeroReturnsStart() {
    assertThat(newSolution().addBusinessDays(SATURDAY, 0, Set.of(MONDAY))).isEqualTo(SATURDAY);
  }

  @Test
  @DisplayName("null-аргументы отклоняются")
  void validation() {
    assertThatThrownBy(() -> newSolution().addBusinessDays(null, 1, Set.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().addBusinessDays(FRIDAY, 1, null))
        .isInstanceOf(NullPointerException.class);
  }
}
