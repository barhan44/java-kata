package dev.kata.solutions.easy;

import dev.kata.easy.Easy39_PriorityTaskScheduler;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.stream.IntStream;

/**
 * Эталон Easy 39.
 *
 * <p>Компаратор {@code comparingInt(priority).reversed().thenComparingLong(createdAt)
 * .thenComparing(id)} выражает бизнес-правило целиком: высокий приоритет, затем более старая
 * задача, затем стабильный tie-break по id. {@link PriorityQueue} хранит задачи в куче и даёт O(log
 * n) на добавление/извлечение.
 *
 * <p>{@code cancelById} использует {@code removeIf}: PriorityQueue не умеет искать по id за O(log
 * n), но для учебной очереди O(n) приемлемо. {@code drainTop} снимает {@code min(n, size)}
 * элементов через {@code IntStream.range}; результат неизменяем.
 */
public final class Easy39_PriorityTaskSchedulerSolution extends Easy39_PriorityTaskScheduler {

  private static final Comparator<Job> ORDER =
      Comparator.comparingInt(Job::priority)
          .reversed()
          .thenComparingLong(Job::createdAt)
          .thenComparing(Job::id);

  @Override
  public JobQueue newJobQueue() {
    return new PriorityJobQueue();
  }

  private static final class PriorityJobQueue extends JobQueue {

    private final PriorityQueue<Job> jobs = new PriorityQueue<>(ORDER);

    @Override
    public void enqueue(Job job) {
      jobs.add(Objects.requireNonNull(job, "job"));
    }

    @Override
    public Optional<Job> dequeue() {
      return Optional.ofNullable(jobs.poll());
    }

    @Override
    public boolean cancelById(String id) {
      Objects.requireNonNull(id, "id");
      if (id.isBlank()) {
        throw new IllegalArgumentException("id must not be blank");
      }
      return jobs.removeIf(job -> job.id().equals(id));
    }

    @Override
    public List<Job> drainTop(int n) {
      if (n < 0) {
        throw new IllegalArgumentException("n must be >= 0, got " + n);
      }
      int count = Math.min(n, jobs.size());
      return IntStream.range(0, count).mapToObj(ignored -> jobs.poll()).toList();
    }
  }
}
