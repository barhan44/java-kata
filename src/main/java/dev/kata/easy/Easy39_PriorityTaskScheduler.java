package dev.kata.easy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Easy 39. Планировщик задач на PriorityQueue.
 *
 * <h2>Задача</h2>
 *
 * <p>Реализовать {@link JobQueue}: {@code enqueue} добавляет задачу, {@code dequeue} достаёт
 * приоритетную, {@code cancelById} удаляет задачу по id, {@code drainTop(n)} снимает до {@code n}
 * самых приоритетных. Выше {@code priority} — раньше; при равном приоритете раньше задача с меньшим
 * {@code createdAt}; при полном совпадении — по {@code id} для детерминированности.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * enqueue(Job("low", 1, 10)); enqueue(Job("high", 5, 20)); enqueue(Job("med", 3, 15))
 * dequeue → Optional[high], затем med, затем low
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>{@code id} не {@code null}/не blank; {@code priority} и {@code createdAt} — любые;
 *   <li>{@code n ≥ 0}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>{@code enqueue}/{@code dequeue} — O(log n); {@code cancelById} — O(n), потому что {@link
 * java.util.PriorityQueue} не индексирует элементы по ключу; {@code drainTop(k)} — O(k log n).
 * Память O(n). Компаратор собирается один раз.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy39_PriorityTaskSchedulerSolution}.
 */
public class Easy39_PriorityTaskScheduler {

  /** Задача. */
  public record Job(String id, int priority, long createdAt) {
    public Job {
      Objects.requireNonNull(id, "id");
      if (id.isBlank()) {
        throw new IllegalArgumentException("id must not be blank");
      }
    }
  }

  /** Очередь приоритетных задач. */
  public static class JobQueue {

    protected JobQueue() {}

    /** Добавляет задачу. */
    public void enqueue(Job job) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Извлекает задачу с наивысшим приоритетом. */
    public Optional<Job> dequeue() {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Удаляет все задачи с указанным id; возвращает true, если что-то удалено. */
    public boolean cancelById(String id) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Извлекает до {@code n} самых приоритетных задач. */
    public List<Job> drainTop(int n) {
      throw new UnsupportedOperationException("Not implemented yet");
    }
  }

  /** Фабрика очереди: тесты выбирают заготовку или эталон. */
  public JobQueue newJobQueue() {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
