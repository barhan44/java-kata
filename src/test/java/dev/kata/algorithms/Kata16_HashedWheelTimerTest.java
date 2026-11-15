package dev.kata.algorithms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import dev.kata.algorithms.Kata16_HashedWheelTimer.TimeoutHandle;
import dev.kata.solutions.algorithms.Kata16_HashedWheelTimerSolution;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 16.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata16* -DuseSolution=true</pre>
 */
@DisplayName("Kata 16: hashed wheel timer")
class Kata16_HashedWheelTimerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata16_HashedWheelTimer newTimer(int ticksPerWheel, Duration tickDuration) {
    return USE_SOLUTION
        ? new Kata16_HashedWheelTimerSolution(ticksPerWheel, tickDuration)
        : new Kata16_HashedWheelTimer(ticksPerWheel, tickDuration);
  }

  @Test
  @DisplayName("Задача срабатывает после задержки и не раньше неё")
  void firesAfterDelay() throws Exception {
    try (Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10))) {
      timer.start();
      CountDownLatch fired = new CountDownLatch(1);
      long startedAt = System.nanoTime();
      Duration delay = Duration.ofMillis(40);

      TimeoutHandle handle = timer.schedule(fired::countDown, delay);

      assertThat(fired.await(3, TimeUnit.SECONDS)).isTrue();
      long elapsedNanos = System.nanoTime() - startedAt;
      assertThat(elapsedNanos)
          .as("задача не должна срабатывать раньше дедлайна")
          .isGreaterThanOrEqualTo(delay.toNanos() - TimeUnit.MILLISECONDS.toNanos(2));
      assertThat(handle.isExpired()).isTrue();
      assertThat(handle.cancel()).isFalse();
    }
  }

  @Test
  @DisplayName("Нулевая задержка срабатывает на следующем тике")
  void zeroDelayFiresOnNextTick() throws Exception {
    try (Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10))) {
      timer.start();
      CountDownLatch fired = new CountDownLatch(1);

      timer.schedule(fired::countDown, Duration.ZERO);

      assertThat(fired.await(2, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("Задачи на разных горизонтах: внутри оборота и через несколько оборотов")
  void firesAcrossMultipleRounds() throws Exception {
    try (Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10))) {
      timer.start();
      long startedAt = System.nanoTime();
      long[] delaysNanos = {
        TimeUnit.MILLISECONDS.toNanos(20),
        TimeUnit.MILLISECONDS.toNanos(120),
        TimeUnit.MILLISECONDS.toNanos(300)
      };
      AtomicLong[] firedAt = {new AtomicLong(-1), new AtomicLong(-1), new AtomicLong(-1)};
      CountDownLatch fired = new CountDownLatch(delaysNanos.length);

      for (int i = 0; i < delaysNanos.length; i++) {
        int index = i;
        timer.schedule(
            () -> {
              firedAt[index].set(System.nanoTime());
              fired.countDown();
            },
            Duration.ofNanos(delaysNanos[i]));
      }

      assertThat(fired.await(5, TimeUnit.SECONDS)).isTrue();
      for (int i = 0; i < delaysNanos.length; i++) {
        long elapsed = firedAt[i].get() - startedAt;
        assertThat(elapsed)
            .as("задержка %d", i)
            .isGreaterThanOrEqualTo(delaysNanos[i] - TimeUnit.MILLISECONDS.toNanos(2));
      }
    }
  }

  @Test
  @DisplayName("Колесо из одного бакета корректно отрабатывает несколько оборотов")
  void singleBucketWheel() throws Exception {
    try (Kata16_HashedWheelTimer timer = newTimer(1, Duration.ofMillis(10))) {
      timer.start();
      CountDownLatch fired = new CountDownLatch(1);

      timer.schedule(fired::countDown, Duration.ofMillis(45));

      assertThat(fired.await(3, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("Отмена до срабатывания: задача не выполняется, isExpired ложно")
  void cancelBeforeFiring() throws Exception {
    try (Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10))) {
      timer.start();
      AtomicBoolean ran = new AtomicBoolean();
      CountDownLatch marker = new CountDownLatch(1);

      TimeoutHandle handle = timer.schedule(() -> ran.set(true), Duration.ofMillis(150));
      timer.schedule(marker::countDown, Duration.ofMillis(300));

      assertThat(handle.cancel()).isTrue();
      assertThat(handle.isExpired()).isFalse();
      assertThat(handle.cancel()).isFalse();

      assertThat(marker.await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(ran).isFalse();
    }
  }

  @Test
  @DisplayName("После срабатывания cancel возвращает false, isExpired — true")
  void cancelAfterExpiry() throws Exception {
    try (Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10))) {
      timer.start();
      CountDownLatch fired = new CountDownLatch(1);
      TimeoutHandle handle = timer.schedule(fired::countDown, Duration.ofMillis(20));

      assertThat(fired.await(3, TimeUnit.SECONDS)).isTrue();
      await().atMost(Duration.ofSeconds(2)).until(handle::isExpired);
      assertThat(handle.cancel()).isFalse();
    }
  }

  @Test
  @DisplayName("stop отменяет ожидающие задачи и запрещает новый schedule")
  void stopCancelsPendingTasks() throws Exception {
    Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10));
    timer.start();
    AtomicBoolean ran = new AtomicBoolean();
    TimeoutHandle handle = timer.schedule(() -> ran.set(true), Duration.ofSeconds(5));

    timer.stop();
    timer.stop();

    assertThat(handle.isExpired()).isFalse();
    assertThat(handle.cancel()).isFalse();
    Thread.sleep(100);
    assertThat(ran).isFalse();
    assertThatThrownBy(() -> timer.schedule(() -> {}, Duration.ofMillis(1)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("schedule до start и start после stop запрещены")
  void lifecycleValidation() {
    Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10));

    assertThatThrownBy(() -> timer.schedule(() -> {}, Duration.ofMillis(1)))
        .isInstanceOf(IllegalStateException.class);

    timer.start();
    timer.start();
    timer.stop();

    assertThatThrownBy(timer::start).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> timer.schedule(() -> {}, Duration.ofMillis(1)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Исключение в задаче не убивает тикер")
  void taskFailureDoesNotKillTicker() throws Exception {
    try (Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10))) {
      timer.start();
      CountDownLatch afterFailure = new CountDownLatch(1);

      timer.schedule(
          () -> {
            throw new IllegalStateException("boom");
          },
          Duration.ofMillis(10));
      timer.schedule(afterFailure::countDown, Duration.ofMillis(50));

      assertThat(afterFailure.await(3, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("Задача может планировать новую задачу")
  void taskCanScheduleAnotherTask() throws Exception {
    try (Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10))) {
      timer.start();
      CountDownLatch second = new CountDownLatch(1);

      timer.schedule(
          () -> timer.schedule(second::countDown, Duration.ofMillis(20)), Duration.ofMillis(10));

      assertThat(second.await(3, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("stop изнутри задачи не вешает тикер")
  void stopFromTask() throws Exception {
    Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10));
    timer.start();
    CountDownLatch stopped = new CountDownLatch(1);

    timer.schedule(
        () -> {
          timer.stop();
          stopped.countDown();
        },
        Duration.ofMillis(10));

    assertThat(stopped.await(3, TimeUnit.SECONDS)).isTrue();
    timer.stop();
  }

  @Test
  @DisplayName("Массовое планирование 50 000 таймеров")
  void schedulesFiftyThousandTimers() throws Exception {
    int taskCount = 50_000;
    try (Kata16_HashedWheelTimer timer = newTimer(1024, Duration.ofMillis(2))) {
      timer.start();
      CountDownLatch fired = new CountDownLatch(taskCount);

      for (int i = 0; i < taskCount; i++) {
        timer.schedule(fired::countDown, Duration.ofMillis(i % 100));
      }

      assertThat(fired.await(15, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("Валидация конфигурации и аргументов")
  void invalidArguments() {
    assertThatThrownBy(() -> newTimer(0, Duration.ofMillis(10)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newTimer(-1, Duration.ofMillis(10)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newTimer(3, Duration.ofMillis(10)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newTimer(6, Duration.ofMillis(10)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newTimer(8, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newTimer(8, Duration.ofMillis(-1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newTimer(8, null)).isInstanceOf(NullPointerException.class);

    try (Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10))) {
      timer.start();
      assertThatThrownBy(() -> timer.schedule(null, Duration.ofMillis(1)))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> timer.schedule(() -> {}, null))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> timer.schedule(() -> {}, Duration.ofMillis(-1)))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  @DisplayName("Отменённые задачи не попадают в выборку сработавших")
  void cancelledHandlesAreNotExpired() throws Exception {
    try (Kata16_HashedWheelTimer timer = newTimer(8, Duration.ofMillis(10))) {
      timer.start();
      List<TimeoutHandle> handles = new ArrayList<>();

      for (int i = 0; i < 100; i++) {
        handles.add(timer.schedule(() -> {}, Duration.ofMillis(500)));
      }
      for (TimeoutHandle handle : handles) {
        assertThat(handle.cancel()).isTrue();
        assertThat(handle.isExpired()).isFalse();
      }
    }
  }
}
