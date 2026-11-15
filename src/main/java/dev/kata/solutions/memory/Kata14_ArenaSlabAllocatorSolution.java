package dev.kata.solutions.memory;

import dev.kata.memory.Kata14_ArenaSlabAllocator;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Эталонное решение Kata 14.
 *
 * <h2>Одна арена — много слотов</h2>
 *
 * <p>В конструкторе создаётся {@link Arena#ofShared()} и один {@link MemorySegment} размером {@code
 * slotSizeBytes * slotCount}. Shared arena, а не confined, потому что слоты могут читать и писать
 * разные потоки: confined arena разрешает доступ только создавшему потоку. Все слоты лежат в одном
 * сегменте, поэтому аллокатор не делает системных вызовов на каждое выделение.
 *
 * <h2>Intrusive free-list</h2>
 *
 * <p>Первые 8 байт каждого свободного слота хранят индекс следующего свободного слота ({@code -1} —
 * конец списка). {@code freeHead} — голова списка. Выделение:
 *
 * <pre>
 *   slot = freeHead;
 *   freeHead = segment.get(JAVA_LONG, slot * slotSize);
 *   markAllocated(slot);
 * </pre>
 *
 * <p>Освобождение выполняет обратную операцию и пишет в слот старую голову. Это O(1) и не создаёт
 * ни одного Java-объекта.
 *
 * <h2>Битсет против double-free</h2>
 *
 * <p>{@code long[] allocated} — примитивный битсет, созданный один раз. Перед освобождением
 * проверяется бит: если он нулевой, слот уже свободен (или никогда не выделялся), и мы бросаем
 * {@link IllegalStateException}. Без битсета повторный {@code free} записал бы в свободный слот
 * новый {@code next}, перезаписав служебную ссылку, и free-list был бы повреждён — классическая
 * ошибка работы с intrusive-списками. Битсет также защищает от освобождения слота, который не
 * выдавался этим аллокатором (например, пользователь передал случайный индекс).
 *
 * <h2>Блокировка</h2>
 *
 * <p>Метаданные (голова, битсет, служебные ссылки) защищены {@link ReentrantLock}. Критическая
 * секция — несколько обращений к памяти, десятки наносекунд. Пользовательская работа со срезом
 * выполняется без блокировки: аллокатор не знает, что именно владелец пишет в слот. CAS-версия
 * возможна, но требует атомарного битсета и аккуратной ABA-защиты при возврате слотов; для учебного
 * решения важнее прозрачность и гарантированная корректность.
 *
 * <h2>Zero-GC и оговорки</h2>
 *
 * <p>{@code allocate}/{@code free} не аллоцируют: {@link MemorySegment#get}/{@link
 * MemorySegment#set} работают с off-heap напрямую, {@code ReentrantLock} не создаёт мусора, а
 * исключения возникают только на ошибочном пути. {@link #slice(long)} создаёт Java-обёртку {@link
 * MemorySegment}: это неизбежная цена API, но данные остаются в off-heap и не копируются.
 *
 * <p>Слот после {@code free} может быть выдан другому потоку. Аллокатор не может «отозвать» уже
 * выданный срез, поэтому контракт требует не использовать срез после освобождения. Аналогично,
 * закрывать арену нужно после завершения всех операций со срезами.
 */
public final class Kata14_ArenaSlabAllocatorSolution extends Kata14_ArenaSlabAllocator {

  /** Смещение служебной ссылки внутри свободного слота. */
  private static final long FREE_NEXT_OFFSET = 0L;

  private final ReentrantLock lock = new ReentrantLock();
  private final Arena arena;
  private final MemorySegment data;
  private final long[] allocatedBits;
  private long freeHead;
  private boolean closed;

  public Kata14_ArenaSlabAllocatorSolution(int slotSizeBytes, int slotCount) {
    super(slotSizeBytes, slotCount);
    if (slotSizeBytes < Long.BYTES || slotSizeBytes % Long.BYTES != 0) {
      throw new IllegalArgumentException(
          "slotSizeBytes must be a multiple of 8 and >= 8, got " + slotSizeBytes);
    }
    if (slotCount <= 0) {
      throw new IllegalArgumentException("slotCount must be > 0, got " + slotCount);
    }

    long totalBytes = (long) slotSizeBytes * slotCount;
    if (totalBytes <= 0) {
      throw new IllegalArgumentException("total size overflows long: " + totalBytes);
    }

    this.arena = Arena.ofShared();
    try {
      this.data = arena.allocate(totalBytes, Long.BYTES);
    } catch (RuntimeException | Error failure) {
      arena.close();
      throw failure;
    }
    this.allocatedBits = new long[(int) (((long) slotCount + 63L) >>> 6)];

    // Строим intrusive free-list: 0 -> 1 -> 2 -> ... -> -1.
    for (int slot = 0; slot < slotCount; slot++) {
      long next = slot + 1L < slotCount ? slot + 1L : -1L;
      data.set(ValueLayout.JAVA_LONG, (long) slot * slotSizeBytes + FREE_NEXT_OFFSET, next);
    }
    this.freeHead = 0L;
  }

  @Override
  public long allocate() {
    lock.lock();
    try {
      ensureOpen();
      if (freeHead < 0L) {
        return -1L;
      }
      long slot = freeHead;
      freeHead = data.get(ValueLayout.JAVA_LONG, slot * slotSizeBytes + FREE_NEXT_OFFSET);
      setAllocated(slot, true);
      return slot;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void free(long slotId) {
    lock.lock();
    try {
      ensureOpen();
      checkRange(slotId);
      if (!isAllocated(slotId)) {
        throw new IllegalStateException(
            "slot " + slotId + " is not allocated (double-free or invalid owner)");
      }
      setAllocated(slotId, false);
      data.set(ValueLayout.JAVA_LONG, slotId * slotSizeBytes + FREE_NEXT_OFFSET, freeHead);
      freeHead = slotId;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public MemorySegment slice(long slotId) {
    lock.lock();
    try {
      ensureOpen();
      checkRange(slotId);
      if (!isAllocated(slotId)) {
        throw new IllegalStateException("slot " + slotId + " is not allocated");
      }
      return data.asSlice(slotId * slotSizeBytes, slotSizeBytes);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void close() {
    lock.lock();
    try {
      if (closed) {
        return;
      }
      // Сначала закрываем арену: если FFM обнаружит конкурентный доступ к срезу и бросит
      // исключение, аллокатор останется открытым, и close можно будет повторить.
      arena.close();
      closed = true;
    } finally {
      lock.unlock();
    }
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("allocator is closed");
    }
  }

  private void checkRange(long slotId) {
    if (slotId < 0L || slotId >= slotCount) {
      throw new IndexOutOfBoundsException(
          "slotId must be in [0, " + slotCount + "), got " + slotId);
    }
  }

  private void setAllocated(long slotId, boolean allocated) {
    int word = (int) (slotId >>> 6);
    long mask = 1L << (int) (slotId & 63L);
    if (allocated) {
      allocatedBits[word] |= mask;
    } else {
      allocatedBits[word] &= ~mask;
    }
  }

  private boolean isAllocated(long slotId) {
    int word = (int) (slotId >>> 6);
    long mask = 1L << (int) (slotId & 63L);
    return (allocatedBits[word] & mask) != 0L;
  }
}
