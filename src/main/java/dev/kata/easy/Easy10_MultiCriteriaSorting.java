package dev.kata.easy;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Easy 10. Многокритериальная сортировка задач (Comparator chaining).
 *
 * <h2>Задача</h2>
 *
 * <p>Отсортировать {@link Task} по четырём критериям:
 *
 * <ol>
 *   <li>невыполненные раньше выполненных;
 *   <li>приоритет {@code HIGH → MEDIUM → LOW};
 *   <li>дедлайн: ближайшие раньше, {@code null} всегда в конце;
 *   <li>при полном совпадении — {@code title} без учёта регистра.
 * </ol>
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * [Task("B", HIGH, 2026-01-02, false),
 *  Task("A", HIGH, null, false),
 *  Task("C", LOW, 2026-01-01, false),
 *  Task("D", HIGH, 2026-01-01, true)]
 *
 * → [B, A, C, D]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code tasks.size()};
 *   <li>{@code title} не {@code null}/не blank, {@code priority} не {@code null}, {@code dueDate}
 *       может быть {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n log n), память O(n) на результат (сортировка копии, вход не меняется). Эталон —
 * цепочка {@code Comparator.comparing(...).thenComparing(...)} с {@code nullsLast} и {@code
 * String.CASE_INSENSITIVE_ORDER}; ни одного ручного {@code if} в компараторе.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy10_MultiCriteriaSortingSolution}.
 */
public class Easy10_MultiCriteriaSorting {

  /** Приоритет; порядок объявления задаёт порядок сортировки. */
  public enum Priority {
    HIGH,
    MEDIUM,
    LOW
  }

  /** Задача. */
  public record Task(String title, Priority priority, LocalDate dueDate, boolean completed) {
    public Task {
      Objects.requireNonNull(title, "title");
      if (title.isBlank()) {
        throw new IllegalArgumentException("title must not be blank");
      }
      Objects.requireNonNull(priority, "priority");
    }
  }

  /** Сортирует задачи по бизнес-критериям. */
  public List<Task> sortTasks(List<Task> tasks) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
