package dev.kata.solutions.distributed;

import dev.kata.distributed.Kata23_HybridLogicalClock;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Эталонное решение Kata 23.
 *
 * <h2>Почему метка строго растёт</h2>
 *
 * <p>Для локального события {@code now()}:
 *
 * <ul>
 *   <li>если физические часы ушли вперёд ({@code pt > l}), новая метка имеет большее физическое
 *       время — она больше предыдущей;
 *   <li>иначе {@code l} не меняется, а {@code c} увеличивается на 1; лексикографический порядок
 *       {@code (l, c)} даёт строгое возрастание;
 *   <li>при {@code c == 65535} увеличивается {@code l}, а {@code c} сбрасывается в 0 — снова строго
 *       больше.
 * </ul>
 *
 * <p>Для {@code update(received)} берётся {@code l' = max(l, received.l, pt)}. Если {@code l'}
 * больше одной из сторон, физическая компонента уже обеспечивает строгое превосходство. Если {@code
 * l'} совпадает с обеими, счётчик становится {@code max(c, received.c) + 1}, что больше обеих
 * меток. Переполнение счётчика, как и в {@code now()}, увеличивает {@code l'}.
 *
 * <h2>Почему это работает при откате часов</h2>
 *
 * <p>Мы никогда не уменьшаем {@code l}. Если NTP сдвинул физические часы назад, {@code pt < l}, и
 * новые события получают тот же {@code l} с большим {@code c}. Логическая компонента «держит»
 * монотонность, пока реальное время не догонит сохранённое. Это и есть hybrid: физическая часть
 * отражает реальное время, логическая — причинно-следственный порядок.
 *
 * <h2>Упаковка</h2>
 *
 * <pre>
 *   packed = (physicalTimeMillis << 16) | (logicalCounter &amp; 0xFFFF)
 *   physical = packed >>> 16
 *   counter  = (int) (packed &amp; 0xFFFF)
 * </pre>
 *
 * <p>48 + 16 = 64 бита. {@code >>>} (беззнаковый сдвиг) важен: при больших физических временах
 * старший бит {@code long} может быть установлен, но 48-битная компонента всё равно извлекается
 * корректно. Для любого {@code long x} {@code pack(unpack(x)) == x}; это проверяется в тестах.
 *
 * <h2>Zero-allocation и потокобезопасность</h2>
 *
 * <p>{@code nowPacked}/{@code updatePacked} работают только с примитивами и не создают объектов —
 * это горячий путь для каждого сообщения/события (см. JMH). {@link HlcTimestamp} создаётся только в
 * удобных {@code now()}/{@code update()}. Все методы синхронизированы: состояние — пара {@code (l,
 * c)}, и обновлять её нужно атомарно. Для одного узла это дешевле, чем CAS-цикл с упакованным long,
 * и не создаёт ложного оптимизма при переполнении.
 */
public final class Kata23_HybridLogicalClockSolution extends Kata23_HybridLogicalClock {

  public Kata23_HybridLogicalClockSolution(LongSupplier physicalClock) {
    super(physicalClock);
    Objects.requireNonNull(physicalClock, "physicalClock");
  }

  @Override
  public synchronized HlcTimestamp now() {
    return unpack(nowPacked());
  }

  @Override
  public synchronized HlcTimestamp update(HlcTimestamp receivedTimestamp) {
    Objects.requireNonNull(receivedTimestamp, "receivedTimestamp");
    return unpack(updatePacked(pack(receivedTimestamp)));
  }

  @Override
  public synchronized long nowPacked() {
    long physical = physicalClock.getAsLong();
    checkPhysical(physical);

    if (physical > physicalTimeMillis) {
      physicalTimeMillis = physical;
      logicalCounter = 0;
    } else if (logicalCounter < MAX_LOGICAL_COUNTER) {
      logicalCounter++;
    } else {
      physicalTimeMillis = nextPhysical(physicalTimeMillis);
      logicalCounter = 0;
    }

    return packInternal(physicalTimeMillis, logicalCounter);
  }

  @Override
  public synchronized long updatePacked(long receivedPacked) {
    long receivedPhysical = receivedPacked >>> 16;
    int receivedCounter = (int) (receivedPacked & MAX_LOGICAL_COUNTER);

    long physical = physicalClock.getAsLong();
    checkPhysical(physical);

    long newPhysical = Math.max(Math.max(physicalTimeMillis, receivedPhysical), physical);
    int newCounter;

    if (newPhysical == physicalTimeMillis && newPhysical == receivedPhysical) {
      int maxCounter = Math.max(logicalCounter, receivedCounter);
      if (maxCounter < MAX_LOGICAL_COUNTER) {
        newCounter = maxCounter + 1;
      } else {
        newPhysical = nextPhysical(newPhysical);
        newCounter = 0;
      }
    } else if (newPhysical == physicalTimeMillis) {
      if (logicalCounter < MAX_LOGICAL_COUNTER) {
        newCounter = logicalCounter + 1;
      } else {
        newPhysical = nextPhysical(newPhysical);
        newCounter = 0;
      }
    } else if (newPhysical == receivedPhysical) {
      if (receivedCounter < MAX_LOGICAL_COUNTER) {
        newCounter = receivedCounter + 1;
      } else {
        newPhysical = nextPhysical(newPhysical);
        newCounter = 0;
      }
    } else {
      newCounter = 0;
    }

    physicalTimeMillis = newPhysical;
    logicalCounter = newCounter;
    return packInternal(newPhysical, newCounter);
  }

  public static long pack(HlcTimestamp timestamp) {
    Objects.requireNonNull(timestamp, "timestamp");
    return (timestamp.physicalTimeMillis() << 16)
        | (timestamp.logicalCounter() & (long) MAX_LOGICAL_COUNTER);
  }

  public static HlcTimestamp unpack(long packed) {
    long physical = packed >>> 16;
    int counter = (int) (packed & MAX_LOGICAL_COUNTER);
    return new HlcTimestamp(physical, counter);
  }

  private static long packInternal(long physical, int counter) {
    return (physical << 16) | (counter & (long) MAX_LOGICAL_COUNTER);
  }

  private static void checkPhysical(long physical) {
    if (physical < 0L || physical > MAX_PHYSICAL_TIME) {
      throw new IllegalArgumentException(
          "physical time must be in [0, " + MAX_PHYSICAL_TIME + "], got " + physical);
    }
  }

  private static long nextPhysical(long physical) {
    if (physical >= MAX_PHYSICAL_TIME) {
      throw new IllegalArgumentException("HLC physical time overflow");
    }
    return physical + 1L;
  }
}
