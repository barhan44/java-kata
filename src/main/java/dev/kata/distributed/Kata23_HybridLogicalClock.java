package dev.kata.distributed;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Kata 23. Hybrid Logical Clock (HLC) для упорядочивания событий в распределённой системе.
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Физические часы узлов расходятся из-за NTP-коррекций и дрейфа, а чисто логические часы
 * (Lamport) не имеют отношения к реальному времени. HLC (Kulkarni et al., CockroachDB, YugabyteDB)
 * совмещает оба: метка содержит физическое время и логический счётчик и гарантирует строгую
 * монотонность даже при откате часов назад. По метке можно сравнивать события, строить snapshot'ы и
 * ограничивать время ожидания, не полагаясь на идеальную синхронизацию.
 *
 * <h2>Состояние и алгоритм</h2>
 *
 * <pre>
 *   state = (l: physicalTimeMillis, c: logicalCounter)
 *
 *   now():
 *     pt = physicalClock()
 *     if pt > l:  l = pt; c = 0
 *     else:       c++   (при переполнении 16 бит: l++, c = 0)
 *     return (l, c)
 *
 *   update(received):
 *     pt = physicalClock()
 *     l' = max(l, received.l, pt)
 *     c' = max(c, received.c) + 1, если l' == l == received.l
 *          c + 1,                если l' == l
 *          received.c + 1,       если l' == received.l
 *          0,                    если l' == pt (физическое время ушло вперёд)
 *     state = (l', c')
 * </pre>
 *
 * <p>Результат {@code now()} строго больше предыдущей локальной метки, а {@code update()} — строго
 * больше и локальной, и полученной. При откате физических часов {@code l} не уменьшается: растёт
 * логический счётчик, а при его переполнении — физическая компонента.
 *
 * <h2>Упаковка в long</h2>
 *
 * <p>48 бит физического времени (миллисекунды, максимум {@link #MAX_PHYSICAL_TIME}) и 16 бит
 * логического счётчика ({@link #MAX_LOGICAL_COUNTER}). {@link #nowPacked()} и {@link
 * #updatePacked(long)} работают без создания объектов; {@link #pack(HlcTimestamp)} и {@link
 * #unpack(long)} — статические конвертеры. Для любого {@code long x} выполняется {@code
 * pack(unpack(x)) == x}.
 *
 * <h2>Требования</h2>
 *
 * <ol>
 *   <li>{@link HlcTimestamp} реализует {@link Comparable}; порядок — сначала физическое время,
 *       затем счётчик.
 *   <li>Методы часов потокобезопасны: состояние обновляется под синхронизацией.
 *   <li>{@code physicalClock} инжектируется ({@link LongSupplier}), поэтому тесты управляют
 *       временем без {@code Thread.sleep}; часы должны возвращать миллисекунды и не обязаны быть
 *       монотонными.
 *   <li>Физическое время должно помещаться в 48 бит; при переполнении счётчика и физической
 *       компоненты — {@link IllegalArgumentException}.
 * </ol>
 *
 * <h2>Сложность</h2>
 *
 * <ul>
 *   <li>{@code now}/{@code update} — O(1).
 *   <li>{@code nowPacked}/{@code updatePacked} — O(1) и zero-allocation.
 * </ul>
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>{@code null} physicalClock / received timestamp — {@link NullPointerException}.
 *   <li>{@code physicalTimeMillis < 0} или {@code > MAX_PHYSICAL_TIME}, счётчик вне {@code [0,
 *       65535]} — {@link IllegalArgumentException}.
 *   <li>Счётчик 65535: следующий локальный тик увеличивает физическую компоненту на 1 и сбрасывает
 *       счётчик.
 *   <li>Полученная метка из «будущего»: локальные часы переходят на её физическое время, счётчик
 *       становится {@code received.counter + 1}.
 * </ul>
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.distributed.Kata23_HybridLogicalClockSolution}.
 * Тесты: {@code Kata23_HybridLogicalClockTest}.
 */
public class Kata23_HybridLogicalClock {

  /** Максимум 48-битной физической компоненты. */
  public static final long MAX_PHYSICAL_TIME = (1L << 48) - 1L;

  /** Максимум 16-битного логического счётчика. */
  public static final int MAX_LOGICAL_COUNTER = 0xFFFF;

  /** Метка HLC: физическое время + логический счётчик. */
  public record HlcTimestamp(long physicalTimeMillis, int logicalCounter)
      implements Comparable<HlcTimestamp> {

    public HlcTimestamp {
      if (physicalTimeMillis < 0L || physicalTimeMillis > MAX_PHYSICAL_TIME) {
        throw new IllegalArgumentException(
            "physicalTimeMillis must be in [0, "
                + MAX_PHYSICAL_TIME
                + "], got "
                + physicalTimeMillis);
      }
      if (logicalCounter < 0 || logicalCounter > MAX_LOGICAL_COUNTER) {
        throw new IllegalArgumentException(
            "logicalCounter must be in [0, " + MAX_LOGICAL_COUNTER + "], got " + logicalCounter);
      }
    }

    @Override
    public int compareTo(HlcTimestamp other) {
      Objects.requireNonNull(other, "other");
      int byPhysical = Long.compare(physicalTimeMillis, other.physicalTimeMillis);
      return byPhysical != 0 ? byPhysical : Integer.compare(logicalCounter, other.logicalCounter);
    }
  }

  protected final LongSupplier physicalClock;
  protected long physicalTimeMillis;
  protected int logicalCounter;

  /**
   * @param physicalClock источник физического времени в миллисекундах (может откатываться назад)
   */
  public Kata23_HybridLogicalClock(LongSupplier physicalClock) {
    this.physicalClock = physicalClock;
  }

  /**
   * Генерирует метку локального события.
   *
   * @return строго возрастающая метка
   */
  public synchronized HlcTimestamp now() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Обновляет локальные часы при получении удалённого события.
   *
   * @param receivedTimestamp метка из сообщения
   * @return новая метка, строго большая локальной и полученной
   */
  public synchronized HlcTimestamp update(HlcTimestamp receivedTimestamp) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * @return следующая метка, упакованная в {@code long}, без создания объекта
   */
  public synchronized long nowPacked() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * @param receivedPacked упакованная удалённая метка
   * @return упакованная новая метка
   */
  public synchronized long updatePacked(long receivedPacked) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Упаковывает метку: 48 бит физического времени и 16 бит счётчика.
   *
   * @param timestamp метка
   * @return упакованное значение
   */
  public static long pack(HlcTimestamp timestamp) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Распаковывает метку.
   *
   * @param packed упакованное значение
   * @return метка
   */
  public static HlcTimestamp unpack(long packed) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
