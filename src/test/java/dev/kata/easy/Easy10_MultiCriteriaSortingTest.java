package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy10_MultiCriteriaSorting.Priority;
import dev.kata.easy.Easy10_MultiCriteriaSorting.Task;
import dev.kata.solutions.easy.Easy10_MultiCriteriaSortingSolution;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 10: Multi-criteria task sorting")
class Easy10_MultiCriteriaSortingTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy10_MultiCriteriaSorting newSolution() {
    return USE_SOLUTION
        ? new Easy10_MultiCriteriaSortingSolution()
        : new Easy10_MultiCriteriaSorting();
  }

  @Test
  @DisplayName("Невыполненные, приоритет, дедлайн, nullsLast")
  void sortsByAllCriteria() {
    Task completed = new Task("Done", Priority.HIGH, LocalDate.of(2026, 1, 1), true);
    Task highLate = new Task("Late", Priority.HIGH, LocalDate.of(2026, 2, 1), false);
    Task highNoDate = new Task("NoDate", Priority.HIGH, null, false);
    Task medium = new Task("Medium", Priority.MEDIUM, LocalDate.of(2026, 1, 1), false);
    Task low = new Task("Low", Priority.LOW, LocalDate.of(2026, 1, 1), false);

    List<Task> result =
        newSolution().sortTasks(List.of(completed, low, highNoDate, medium, highLate));

    assertThat(result).containsExactly(highLate, highNoDate, medium, low, completed);
  }

  @Test
  @DisplayName("При полном совпадении — title без учёта регистра")
  void titleTieBreakIsCaseInsensitive() {
    LocalDate date = LocalDate.of(2026, 3, 1);
    Task beta = new Task("beta", Priority.MEDIUM, date, false);
    Task alpha = new Task("Alpha", Priority.MEDIUM, date, false);
    Task gamma = new Task("GAMMA", Priority.MEDIUM, date, false);

    assertThat(newSolution().sortTasks(List.of(beta, gamma, alpha)))
        .containsExactly(alpha, beta, gamma);
  }

  @Test
  @DisplayName("Завершённые всегда после незавершённых")
  void completedGoesLast() {
    Task completedUrgent = new Task("Urgent done", Priority.HIGH, LocalDate.of(2020, 1, 1), true);
    Task pendingLow = new Task("Pending low", Priority.LOW, LocalDate.of(2030, 1, 1), false);

    assertThat(newSolution().sortTasks(List.of(completedUrgent, pendingLow)))
        .containsExactly(pendingLow, completedUrgent);
  }

  @Test
  @DisplayName("Пустой список и один элемент")
  void emptyAndSingle() {
    assertThat(newSolution().sortTasks(List.of())).isEmpty();
    Task task = new Task("Only", Priority.LOW, null, false);
    assertThat(newSolution().sortTasks(List.of(task))).containsExactly(task);
  }

  @Test
  @DisplayName("Исходный список не меняется, результат неизменяем")
  void immutability() {
    List<Task> input = new ArrayList<>();
    Task first = new Task("B", Priority.LOW, null, false);
    Task second = new Task("A", Priority.HIGH, null, false);
    input.add(first);
    input.add(second);

    List<Task> result = newSolution().sortTasks(input);

    assertThat(input).containsExactly(first, second);
    assertThat(result).containsExactly(second, first);
    assertThatThrownBy(() -> result.add(first)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация Task и null-списка")
  void validation() {
    assertThatThrownBy(() -> new Task(null, Priority.HIGH, null, false))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Task(" ", Priority.HIGH, null, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Task("A", null, null, false))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().sortTasks(null))
        .isInstanceOf(NullPointerException.class);
  }
}
