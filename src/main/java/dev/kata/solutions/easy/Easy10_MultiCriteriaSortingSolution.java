package dev.kata.solutions.easy;

import dev.kata.easy.Easy10_MultiCriteriaSorting;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Эталон Easy 10.
 *
 * <p>Компаратор собирается слева направо ровно в порядке бизнес-правил. {@code
 * Comparator.naturalOrder()} для {@code boolean} даёт {@code false < true}, поэтому невыполненные
 * идут первыми. Порядок enum — это порядок объявления {@code HIGH, MEDIUM, LOW}. {@code nullsLast}
 * отправляет задачи без дедлайна в конец, а {@code String.CASE_INSENSITIVE_ORDER} решает ничьи по
 * названию.
 *
 * <p>Сортируется поток, а не исходный список: {@code sorted()} создаёт новый массив и не мутирует
 * вход. {@code toList()} делает результат неизменяемым.
 */
public final class Easy10_MultiCriteriaSortingSolution extends Easy10_MultiCriteriaSorting {

  @Override
  public List<Task> sortTasks(List<Task> tasks) {
    Objects.requireNonNull(tasks, "tasks");

    Comparator<Task> order =
        Comparator.comparing(Task::completed)
            .thenComparing(Task::priority)
            .thenComparing(Task::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::title, String.CASE_INSENSITIVE_ORDER);

    return tasks.stream().sorted(order).toList();
  }
}
