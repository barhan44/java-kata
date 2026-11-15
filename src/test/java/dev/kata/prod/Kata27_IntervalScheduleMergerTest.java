package dev.kata.prod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.prod.Kata27_IntervalScheduleMerger.TimeSlot;
import dev.kata.solutions.prod.Kata27_IntervalScheduleMergerSolution;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 27.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata27* -DuseSolution=true</pre>
 */
@DisplayName("Kata 27: расписание интервалов на NavigableMap")
class Kata27_IntervalScheduleMergerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final Instant BASE = Instant.parse("2026-01-15T10:00:00Z");

  private Kata27_IntervalScheduleMerger newSchedule() {
    return USE_SOLUTION
        ? new Kata27_IntervalScheduleMergerSolution()
        : new Kata27_IntervalScheduleMerger();
  }

  @Test
  @DisplayName("book объединяет пересекающиеся и смежные интервалы")
  void bookMergesOverlapAndAdjacent() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();

    schedule.book(slot(0, 60));
    schedule.book(slot(30, 90));
    assertThat(schedule.busySlots()).containsExactly(slot(0, 90));

    schedule.book(slot(90, 120));
    assertThat(schedule.busySlots()).containsExactly(slot(0, 120));

    schedule.book(slot(130, 140));
    assertThat(schedule.busySlots()).containsExactly(slot(0, 120), slot(130, 140));
  }

  @Test
  @DisplayName("Интервал внутри существующего не меняет занятость")
  void bookInsideExisting() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(0, 120));

    schedule.book(slot(30, 60));

    assertThat(schedule.busySlots()).containsExactly(slot(0, 120));
  }

  @Test
  @DisplayName("Новый интервал соединяет два соседних")
  void bookBridgesGap() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(0, 60));
    schedule.book(slot(90, 120));

    schedule.book(slot(60, 90));

    assertThat(schedule.busySlots()).containsExactly(slot(0, 120));
  }

  @Test
  @DisplayName("Повторное бронирование того же интервала идемпотентно")
  void bookSameSlotTwice() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();

    schedule.book(slot(0, 60));
    schedule.book(slot(0, 60));

    assertThat(schedule.busySlots()).containsExactly(slot(0, 60));
  }

  @Test
  @DisplayName("cancel в середине разрезает интервал на два")
  void cancelMiddleSplits() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(0, 180));

    schedule.cancel(slot(60, 120));

    assertThat(schedule.busySlots()).containsExactly(slot(0, 60), slot(120, 180));
  }

  @Test
  @DisplayName("cancel обрезает левый и правый край, затем удаляет остаток")
  void cancelEdges() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(0, 120));

    schedule.cancel(slot(0, 30));
    assertThat(schedule.busySlots()).containsExactly(slot(30, 120));

    schedule.cancel(slot(90, 120));
    assertThat(schedule.busySlots()).containsExactly(slot(30, 90));

    schedule.cancel(slot(40, 50));
    assertThat(schedule.busySlots()).containsExactly(slot(30, 40), slot(50, 90));

    schedule.cancel(slot(30, 90));
    assertThat(schedule.busySlots()).isEmpty();
  }

  @Test
  @DisplayName("cancel интервала, который лишь касается занятого, — no-op")
  void cancelAdjacentDoesNothing() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(0, 60));
    schedule.book(slot(90, 150));

    schedule.cancel(slot(60, 90));
    assertThat(schedule.busySlots()).containsExactly(slot(0, 60), slot(90, 150));

    schedule.cancel(slot(150, 180));
    assertThat(schedule.busySlots()).containsExactly(slot(0, 60), slot(90, 150));
  }

  @Test
  @DisplayName("cancel может затронуть несколько интервалов сразу")
  void cancelAcrossMultipleIntervals() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(0, 60));
    schedule.book(slot(90, 150));

    schedule.cancel(slot(30, 120));

    assertThat(schedule.busySlots()).containsExactly(slot(0, 30), slot(120, 150));
  }

  @Test
  @DisplayName("findFreeSlots находит все окна в диапазоне")
  void findFreeSlots() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(0, 60));
    schedule.book(slot(90, 150));

    assertThat(schedule.findFreeSlots(at(-30), at(180), Duration.ofMinutes(30)))
        .containsExactly(slot(-30, 0), slot(60, 90), slot(150, 180));
    assertThat(schedule.findFreeSlots(at(0), at(150), Duration.ofMinutes(30)))
        .containsExactly(slot(60, 90));
    assertThat(schedule.findFreeSlots(at(0), at(60), Duration.ofMinutes(1))).isEmpty();
  }

  @Test
  @DisplayName("findFreeSlots: границы диапазона, minDuration и покрытие занятым")
  void findFreeSlotsBoundaries() {
    Kata27_IntervalScheduleMerger empty = newSchedule();
    assertThat(empty.findFreeSlots(at(0), at(60), Duration.ofMinutes(60)))
        .containsExactly(slot(0, 60));
    assertThat(empty.findFreeSlots(at(0), at(60), Duration.ofMinutes(61))).isEmpty();

    Kata27_IntervalScheduleMerger inside = newSchedule();
    inside.book(slot(0, 120));
    assertThat(inside.findFreeSlots(at(30), at(90), Duration.ofMinutes(1))).isEmpty();
    assertThat(inside.findFreeSlots(at(30), at(180), Duration.ofMinutes(1)))
        .containsExactly(slot(120, 180));

    Kata27_IntervalScheduleMerger covers = newSchedule();
    covers.book(slot(-30, 200));
    assertThat(covers.findFreeSlots(at(0), at(180), Duration.ofMinutes(1))).isEmpty();
  }

  @Test
  @DisplayName("findFreeSlots: занятый интервал начинается ровно на границе to")
  void findFreeSlotsBusyStartsAtTo() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(60, 120));

    assertThat(schedule.findFreeSlots(at(0), at(90), Duration.ofMinutes(1)))
        .containsExactly(slot(0, 60));
  }

  @Test
  @DisplayName("findFreeSlots: точная минимальная длительность включается")
  void findFreeSlotsExactMinimum() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(0, 30));
    schedule.book(slot(60, 120));

    assertThat(schedule.findFreeSlots(at(0), at(120), Duration.ofMinutes(30)))
        .containsExactly(slot(30, 60));
    assertThat(schedule.findFreeSlots(at(0), at(120), Duration.ofMinutes(31))).isEmpty();
  }

  @Test
  @DisplayName("busySlots возвращает неизменяемый снимок")
  void busySlotsImmutable() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.book(slot(0, 60));

    List<TimeSlot> slots = schedule.busySlots();
    assertThatThrownBy(() -> slots.add(slot(60, 90)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация TimeSlot и аргументов поиска")
  void validation() {
    assertThatThrownBy(() -> slot(0, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> slot(10, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TimeSlot(null, at(10))).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new TimeSlot(at(10), null)).isInstanceOf(NullPointerException.class);

    Kata27_IntervalScheduleMerger schedule = newSchedule();
    assertThatThrownBy(() -> schedule.book(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> schedule.cancel(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> schedule.findFreeSlots(null, at(10), Duration.ofMinutes(1)))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> schedule.findFreeSlots(at(0), null, Duration.ofMinutes(1)))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> schedule.findFreeSlots(at(0), at(10), null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> schedule.findFreeSlots(at(0), at(0), Duration.ofMinutes(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> schedule.findFreeSlots(at(10), at(0), Duration.ofMinutes(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> schedule.findFreeSlots(at(0), at(10), Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> schedule.findFreeSlots(at(0), at(10), Duration.ofMinutes(-1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("cancel на пустом расписании и за пределами занятости — no-op")
  void cancelOnEmptyOrOutside() {
    Kata27_IntervalScheduleMerger schedule = newSchedule();
    schedule.cancel(slot(0, 60));
    assertThat(schedule.busySlots()).isEmpty();

    schedule.book(slot(100, 160));
    schedule.cancel(slot(0, 100));
    schedule.cancel(slot(160, 200));
    assertThat(schedule.busySlots()).containsExactly(slot(100, 160));
  }

  private static TimeSlot slot(int startMinutes, int endMinutes) {
    return new TimeSlot(at(startMinutes), at(endMinutes));
  }

  private static Instant at(int minutesFromBase) {
    return BASE.plus(Duration.ofMinutes(minutesFromBase));
  }
}
