package dev.kata.solutions.prod;

import dev.kata.prod.Kata27_IntervalScheduleMerger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Эталонное решение Kata 27.
 *
 * <h2>Почему TreeMap start → end</h2>
 *
 * <p>Ключ — начало занятого интервала, значение — конец. Благодаря сортировке {@code floorEntry} и
 * {@code ceilingEntry} находят соседей за O(log K), а не перебором всего списка. Инвариант:
 * интервалы отсортированы, не пересекаются и не являются смежными. Поэтому после любой операции
 * достаточно проверить одного левого соседа и цепочку правых.
 *
 * <h2>book: слияние</h2>
 *
 * <ol>
 *   <li>{@code floorEntry(start)} — интервал с наибольшим началом {@code <= start}. Если его конец
 *       {@code >= start}, он пересекается или касается нового; новый интервал начинается с его
 *       начала, конец — максимум из двух.
 *   <li>Затем {@code ceilingEntry(start)} и все следующие, чей старт {@code <= end}, поглощаются:
 *       конец расширяется, запись удаляется. Каждый удалённый интервал больше не будет обработан.
 * </ol>
 *
 * <p>После слияния ни один сосед не пересекается и не касается нового интервала, инвариант
 * сохранён.
 *
 * <h2>cancel: разрезание</h2>
 *
 * <p>Находим первый занятый интервал, который может пересечься с отменой (floor + ceiling), и идём
 * вправо, пока его начало меньше конца отмены. Для каждого пересечения:
 *
 * <ul>
 *   <li>если он начинается раньше отмены — остаётся левый кусок;
 *   <li>если заканчивается позже отмены — остаётся правый кусок;
 *   <li>полностью покрытый интервал удаляется.
 * </ul>
 *
 * <p>Удаления и вставки применяются после обхода, чтобы не ломать итератор. Касание границы ({@code
 * end == busy.start}) пересечением не считается.
 *
 * <h2>findFreeSlots: один проход по занятым</h2>
 *
 * <p>Курсор начинается с {@code from}. Если {@code from} попал внутрь занятого интервала, курсор
 * сдвигается на его конец. Далее для каждого занятого интервала в диапазоне добавляется свободный
 * промежуток {@code [cursor, busy.start)}, курсор сдвигается на {@code busy.end}, а в конце
 * добавляется {@code [cursor, to)}. Окна короче {@code minDuration} отбрасываются. Сложность O(K +
 * R), без создания промежуточных списков занятых интервалов.
 */
public final class Kata27_IntervalScheduleMergerSolution extends Kata27_IntervalScheduleMerger {

  private final TreeMap<Instant, Instant> busy = new TreeMap<>();

  @Override
  public synchronized void book(TimeSlot slot) {
    Objects.requireNonNull(slot, "slot");

    Instant start = slot.start();
    Instant end = slot.end();

    Map.Entry<Instant, Instant> floor = busy.floorEntry(start);
    if (floor != null && !floor.getValue().isBefore(start)) {
      // Пересечение или смежность слева: [floor.start, floor.end] + [start, end).
      start = floor.getKey();
      if (floor.getValue().isAfter(end)) {
        end = floor.getValue();
      }
      busy.remove(floor.getKey());
    }

    Map.Entry<Instant, Instant> next = busy.ceilingEntry(start);
    while (next != null && !next.getKey().isAfter(end)) {
      if (next.getValue().isAfter(end)) {
        end = next.getValue();
      }
      busy.remove(next.getKey());
      next = busy.ceilingEntry(start);
    }

    busy.put(start, end);
  }

  @Override
  public synchronized void cancel(TimeSlot slot) {
    Objects.requireNonNull(slot, "slot");

    Instant from = slot.start();
    Instant to = slot.end();
    List<Instant> removals = new ArrayList<>();
    List<TimeSlot> remainders = new ArrayList<>();

    Map.Entry<Instant, Instant> entry = busy.floorEntry(from);
    if (entry == null) {
      entry = busy.ceilingEntry(from);
    }

    while (entry != null && entry.getKey().isBefore(to)) {
      Instant busyStart = entry.getKey();
      Instant busyEnd = entry.getValue();
      if (busyEnd.isAfter(from)) {
        removals.add(busyStart);
        if (busyStart.isBefore(from)) {
          remainders.add(new TimeSlot(busyStart, from));
        }
        if (busyEnd.isAfter(to)) {
          remainders.add(new TimeSlot(to, busyEnd));
        }
      }
      entry = busy.higherEntry(busyStart);
    }

    for (Instant start : removals) {
      busy.remove(start);
    }
    for (TimeSlot remainder : remainders) {
      busy.put(remainder.start(), remainder.end());
    }
  }

  @Override
  public synchronized List<TimeSlot> findFreeSlots(Instant from, Instant to, Duration minDuration) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(minDuration, "minDuration");
    if (!from.isBefore(to)) {
      throw new IllegalArgumentException("from must be before to");
    }
    if (minDuration.isZero() || minDuration.isNegative()) {
      throw new IllegalArgumentException("minDuration must be > 0, got " + minDuration);
    }

    List<TimeSlot> free = new ArrayList<>();
    Instant cursor = from;

    Map.Entry<Instant, Instant> entry = busy.floorEntry(from);
    if (entry != null) {
      if (entry.getValue().isAfter(from)) {
        cursor = entry.getValue();
      }
      entry = busy.higherEntry(entry.getKey());
    } else {
      entry = busy.ceilingEntry(from);
    }

    if (!cursor.isBefore(to)) {
      return List.of();
    }

    while (entry != null && entry.getKey().isBefore(to)) {
      Instant busyStart = entry.getKey();
      Instant busyEnd = entry.getValue();
      if (busyStart.isAfter(cursor)) {
        addIfLongEnough(free, cursor, busyStart, minDuration);
      }
      if (busyEnd.isAfter(cursor)) {
        cursor = busyEnd;
      }
      if (!cursor.isBefore(to)) {
        break;
      }
      entry = busy.higherEntry(busyStart);
    }

    if (cursor.isBefore(to)) {
      addIfLongEnough(free, cursor, to, minDuration);
    }
    return List.copyOf(free);
  }

  @Override
  public synchronized List<TimeSlot> busySlots() {
    List<TimeSlot> slots = new ArrayList<>(busy.size());
    for (Map.Entry<Instant, Instant> entry : busy.entrySet()) {
      slots.add(new TimeSlot(entry.getKey(), entry.getValue()));
    }
    return List.copyOf(slots);
  }

  private static void addIfLongEnough(
      List<TimeSlot> free, Instant start, Instant end, Duration minDuration) {
    if (Duration.between(start, end).compareTo(minDuration) >= 0) {
      free.add(new TimeSlot(start, end));
    }
  }
}
