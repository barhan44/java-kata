package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy39_PriorityTaskScheduler.Job;
import dev.kata.easy.Easy39_PriorityTaskScheduler.JobQueue;
import dev.kata.solutions.easy.Easy39_PriorityTaskSchedulerSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 39: Priority task scheduler")
class Easy39_PriorityTaskSchedulerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy39_PriorityTaskScheduler newSolution() {
    return USE_SOLUTION
        ? new Easy39_PriorityTaskSchedulerSolution()
        : new Easy39_PriorityTaskScheduler();
  }

  @Test
  @DisplayName("Извлекает по приоритету, затем по createdAt")
  void dequeuesByPriorityThenTime() {
    JobQueue queue = newSolution().newJobQueue();
    queue.enqueue(new Job("low", 1, 10));
    queue.enqueue(new Job("high", 5, 20));
    queue.enqueue(new Job("med", 3, 15));

    assertThat(queue.dequeue()).contains(new Job("high", 5, 20));
    assertThat(queue.dequeue()).contains(new Job("med", 3, 15));
    assertThat(queue.dequeue()).contains(new Job("low", 1, 10));
    assertThat(queue.dequeue()).isEmpty();
  }

  @Test
  @DisplayName("Равный приоритет: раньше меньший createdAt")
  void tieByCreatedAt() {
    JobQueue queue = newSolution().newJobQueue();
    queue.enqueue(new Job("newer", 1, 20));
    queue.enqueue(new Job("older", 1, 10));

    assertThat(queue.drainTop(2)).containsExactly(new Job("older", 1, 10), new Job("newer", 1, 20));
  }

  @Test
  @DisplayName("cancelById удаляет задачу")
  void cancelsById() {
    JobQueue queue = newSolution().newJobQueue();
    queue.enqueue(new Job("a", 1, 1));
    queue.enqueue(new Job("b", 2, 2));

    assertThat(queue.cancelById("a")).isTrue();
    assertThat(queue.cancelById("missing")).isFalse();
    assertThat(queue.drainTop(10)).containsExactly(new Job("b", 2, 2));
  }

  @Test
  @DisplayName("drainTop: n = 0, n больше размера, порядок")
  void drainTopBoundaries() {
    JobQueue queue = newSolution().newJobQueue();
    queue.enqueue(new Job("a", 1, 1));
    queue.enqueue(new Job("b", 3, 3));
    queue.enqueue(new Job("c", 2, 2));

    assertThat(queue.drainTop(0)).isEmpty();
    assertThat(queue.drainTop(2)).containsExactly(new Job("b", 3, 3), new Job("c", 2, 2));
    assertThat(queue.drainTop(10)).containsExactly(new Job("a", 1, 1));
  }

  @Test
  @DisplayName("Пустая очередь")
  void emptyQueue() {
    JobQueue queue = newSolution().newJobQueue();
    assertThat(queue.dequeue()).isEmpty();
    assertThat(queue.drainTop(3)).isEmpty();
  }

  @Test
  @DisplayName("Результат drainTop неизменяем")
  void immutableDrainResult() {
    JobQueue queue = newSolution().newJobQueue();
    queue.enqueue(new Job("a", 1, 1));

    List<Job> drained = queue.drainTop(1);
    assertThatThrownBy(() -> drained.add(new Job("b", 1, 2)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация Job и аргументов очереди")
  void validation() {
    assertThatThrownBy(() -> new Job(null, 1, 1)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Job(" ", 1, 1)).isInstanceOf(IllegalArgumentException.class);

    JobQueue queue = newSolution().newJobQueue();
    assertThatThrownBy(() -> queue.enqueue(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> queue.cancelById(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> queue.cancelById(" ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> queue.drainTop(-1)).isInstanceOf(IllegalArgumentException.class);
  }
}
