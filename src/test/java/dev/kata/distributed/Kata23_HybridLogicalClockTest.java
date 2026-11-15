package dev.kata.distributed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.distributed.Kata23_HybridLogicalClock.HlcTimestamp;
import dev.kata.solutions.distributed.Kata23_HybridLogicalClockSolution;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 23.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata23* -DuseSolution=true</pre>
 */
@DisplayName("Kata 23: Hybrid Logical Clock")
class Kata23_HybridLogicalClockTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata23_HybridLogicalClock newClock(LongSupplier physicalClock) {
    return USE_SOLUTION
        ? new Kata23_HybridLogicalClockSolution(physicalClock)
        : new Kata23_HybridLogicalClock(physicalClock);
  }

  @Test
  @DisplayName("Локальные события в одной миллисекунде увеличивают логический счётчик")
  void localEventsInSameMillisecond() {
    AtomicLong physical = new AtomicLong(1_000);
    Kata23_HybridLogicalClock clock = newClock(physical::get);

    HlcTimestamp first = clock.now();
    HlcTimestamp second = clock.now();
    HlcTimestamp third = clock.now();

    assertThat(first).isEqualTo(ts(1_000, 0));
    assertThat(second).isEqualTo(ts(1_000, 1));
    assertThat(third).isEqualTo(ts(1_000, 2));
    assertThat(first.compareTo(second)).isNegative();
    assertThat(second.compareTo(third)).isNegative();
  }

  @Test
  @DisplayName("Откат физических часов не нарушает монотонность")
  void clockRollbackKeepsMonotonicity() {
    AtomicLong physical = new AtomicLong(1_000);
    Kata23_HybridLogicalClock clock = newClock(physical::get);

    HlcTimestamp first = clock.now();
    physical.set(500);
    HlcTimestamp second = clock.now();
    physical.set(1_500);
    HlcTimestamp third = clock.now();
    physical.set(100);
    HlcTimestamp fourth = clock.now();

    assertThat(first).isEqualTo(ts(1_000, 0));
    assertThat(second).isEqualTo(ts(1_000, 1));
    assertThat(third).isEqualTo(ts(1_500, 0));
    assertThat(fourth).isEqualTo(ts(1_500, 1));
    assertStrictlyIncreasing(first, second, third, fourth);
  }

  @Test
  @DisplayName("Получение метки из будущего переводит часы на её физическое время")
  void updateFromFuture() {
    AtomicLong physical = new AtomicLong(1_000);
    Kata23_HybridLogicalClock clock = newClock(physical::get);
    HlcTimestamp local = clock.now();

    HlcTimestamp received = ts(2_000, 5);
    HlcTimestamp updated = clock.update(received);

    assertThat(updated).isEqualTo(ts(2_000, 6));
    assertThat(updated.compareTo(local)).isPositive();
    assertThat(updated.compareTo(received)).isPositive();

    assertThat(clock.now()).isEqualTo(ts(2_000, 7));
  }

  @Test
  @DisplayName("update при равном физическом времени берёт максимум счётчиков + 1")
  void updateEqualPhysicalTime() {
    AtomicLong physical = new AtomicLong(1_000);
    Kata23_HybridLogicalClock clock = newClock(physical::get);
    clock.now();

    HlcTimestamp receivedHigh = ts(1_000, 10);
    HlcTimestamp updated = clock.update(receivedHigh);
    assertThat(updated).isEqualTo(ts(1_000, 11));
    assertThat(updated.compareTo(receivedHigh)).isPositive();

    HlcTimestamp receivedLow = ts(1_000, 3);
    HlcTimestamp updatedAgain = clock.update(receivedLow);
    assertThat(updatedAgain).isEqualTo(ts(1_000, 12));
    assertThat(updatedAgain.compareTo(updated)).isPositive();
  }

  @Test
  @DisplayName("update с меткой из прошлого не откатывает локальное время")
  void updateWithLowerReceivedPhysicalTime() {
    AtomicLong physical = new AtomicLong(1_000);
    Kata23_HybridLogicalClock clock = newClock(physical::get);
    clock.now();

    HlcTimestamp received = ts(900, 3);
    HlcTimestamp updated = clock.update(received);

    assertThat(updated).isEqualTo(ts(1_000, 1));
    assertThat(updated.compareTo(received)).isPositive();
  }

  @Test
  @DisplayName("Переполнение счётчика при update увеличивает физическую компоненту")
  void counterOverflowInUpdate() {
    AtomicLong physical = new AtomicLong(1_000);
    Kata23_HybridLogicalClock clock = newClock(physical::get);
    clock.now();

    HlcTimestamp received = ts(1_000, Kata23_HybridLogicalClock.MAX_LOGICAL_COUNTER);
    HlcTimestamp updated = clock.update(received);

    assertThat(updated).isEqualTo(ts(1_001, 0));
    assertThat(updated.compareTo(received)).isPositive();
  }

  @Test
  @DisplayName("70 000 локальных событий строго возрастают, счётчик переполняется с +1 к времени")
  void counterOverflowInNow() {
    AtomicLong physical = new AtomicLong(1_000);
    Kata23_HybridLogicalClock clock = newClock(physical::get);

    long previous = clock.nowPacked();
    boolean strictlyIncreasing = true;
    for (int i = 0; i < 70_000; i++) {
      long next = clock.nowPacked();
      if (next <= previous) {
        strictlyIncreasing = false;
      }
      previous = next;
    }

    assertThat(strictlyIncreasing).isTrue();
    assertThat(unpack(previous).physicalTimeMillis()).isGreaterThan(1_000L);
  }

  @Test
  @DisplayName("Упаковка/распаковка: 48 бит времени + 16 бит счётчика")
  void packingRoundTrip() {
    HlcTimestamp timestamp = ts(123_456_789L, Kata23_HybridLogicalClock.MAX_LOGICAL_COUNTER);
    long packed = pack(timestamp);

    assertThat(unpack(packed)).isEqualTo(timestamp);
    assertThat(packed >>> 16).isEqualTo(timestamp.physicalTimeMillis());
    assertThat((int) (packed & 0xFFFFL)).isEqualTo(timestamp.logicalCounter());
    assertThat(pack(unpack(packed))).isEqualTo(packed);
  }

  @Test
  @DisplayName("pack(unpack(x)) == x для произвольных long")
  void packingIsBijective() {
    for (long value :
        new long[] {
          0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x123456789ABCDEFL, -0x123456789ABCDEFL
        }) {
      assertThat(pack(unpack(value))).isEqualTo(value);
    }
  }

  @Test
  @DisplayName("nowPacked эквивалентен pack(now()) на независимых часах")
  void packedAndObjectApisAgree() {
    AtomicLong physical = new AtomicLong(5_000);
    Kata23_HybridLogicalClock objectClock = newClock(physical::get);
    Kata23_HybridLogicalClock packedClock = newClock(physical::get);

    HlcTimestamp timestamp = objectClock.now();
    long packed = packedClock.nowPacked();

    assertThat(pack(timestamp)).isEqualTo(packed);
  }

  @Test
  @DisplayName("updatePacked эквивалентен pack(update(unpack(received)))")
  void packedAndObjectUpdateAgree() {
    AtomicLong physical = new AtomicLong(5_000);
    Kata23_HybridLogicalClock objectClock = newClock(physical::get);
    Kata23_HybridLogicalClock packedClock = newClock(physical::get);
    HlcTimestamp received = ts(6_000, 7);

    HlcTimestamp updated = objectClock.update(received);
    long packed = packedClock.updatePacked(pack(received));

    assertThat(pack(updated)).isEqualTo(packed);
  }

  @Test
  @DisplayName("Строгая монотонность на случайной последовательности событий")
  void randomEventSequenceIsMonotonic() {
    AtomicLong physical = new AtomicLong(10_000);
    Kata23_HybridLogicalClock clock = newClock(physical::get);
    Random random = new Random(42);
    HlcTimestamp previous = null;

    for (int i = 0; i < 10_000; i++) {
      HlcTimestamp current;
      if (random.nextBoolean()) {
        physical.updateAndGet(value -> Math.max(0L, value + random.nextInt(5) - 2));
        current = clock.now();
      } else {
        long receivedPhysical = Math.max(0L, physical.get() + random.nextInt(20) - 10);
        HlcTimestamp received = ts(receivedPhysical, random.nextInt(10));
        current = clock.update(received);
        assertThat(current.compareTo(received)).isPositive();
      }
      if (previous != null) {
        assertThat(current.compareTo(previous)).isPositive();
      }
      previous = current;
    }
  }

  @Test
  @DisplayName("Сравнение HlcTimestamp: сначала время, затем счётчик")
  void compareTo() {
    assertThat(ts(1, 0).compareTo(ts(2, 0))).isNegative();
    assertThat(ts(2, 0).compareTo(ts(1, 0))).isPositive();
    assertThat(ts(1, 5).compareTo(ts(1, 3))).isPositive();
    assertThat(ts(1, 3).compareTo(ts(1, 3))).isZero();
  }

  @Test
  @DisplayName("Валидация HlcTimestamp и аргументов часов")
  void validation() {
    assertThatThrownBy(() -> ts(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new HlcTimestamp(Kata23_HybridLogicalClock.MAX_PHYSICAL_TIME + 1, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ts(1, -1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new HlcTimestamp(1, Kata23_HybridLogicalClock.MAX_LOGICAL_COUNTER + 1))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> newClock(null)).isInstanceOf(NullPointerException.class);

    Kata23_HybridLogicalClock clock = newClock(() -> 1_000);
    assertThatThrownBy(() -> clock.update(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> pack(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Некорректное физическое время от клок-провайдера отвергается")
  void invalidPhysicalClockValues() {
    Kata23_HybridLogicalClock negative = newClock(() -> -1L);
    assertThatThrownBy(negative::nowPacked).isInstanceOf(IllegalArgumentException.class);

    Kata23_HybridLogicalClock tooLarge =
        newClock(() -> Kata23_HybridLogicalClock.MAX_PHYSICAL_TIME + 1);
    assertThatThrownBy(tooLarge::now).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> tooLarge.updatePacked(0L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Максимальная физическая компонента упаковывается и распаковывается")
  void maxPhysicalTime() {
    HlcTimestamp timestamp = ts(Kata23_HybridLogicalClock.MAX_PHYSICAL_TIME, 0);
    long packed = pack(timestamp);

    assertThat(unpack(packed)).isEqualTo(timestamp);
  }

  private static HlcTimestamp ts(long physical, int logical) {
    return new HlcTimestamp(physical, logical);
  }

  private static long pack(HlcTimestamp timestamp) {
    return USE_SOLUTION
        ? Kata23_HybridLogicalClockSolution.pack(timestamp)
        : Kata23_HybridLogicalClock.pack(timestamp);
  }

  private static HlcTimestamp unpack(long packed) {
    return USE_SOLUTION
        ? Kata23_HybridLogicalClockSolution.unpack(packed)
        : Kata23_HybridLogicalClock.unpack(packed);
  }

  private static void assertStrictlyIncreasing(HlcTimestamp... timestamps) {
    for (int i = 1; i < timestamps.length; i++) {
      assertThat(timestamps[i].compareTo(timestamps[i - 1])).isPositive();
    }
  }
}
