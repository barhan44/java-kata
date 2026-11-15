package dev.kata.solutions.algorithms;

import dev.kata.algorithms.Kata04_RingBuffer;
import java.util.Objects;

/**
 * Эталонное решение Kata 04.
 *
 * <h2>Структура</h2>
 *
 * <pre>
 *   [ padding 7 long ][ producerIndex (volatile) ][ padding 7 long ]
 *   [ padding 7 long ][ consumerIndex (volatile) ][ padding 7 long ]
 *   Object[] slots — массив ссылок, capacity = 2^n
 * </pre>
 *
 * <p>Индексы монотонно растут и никогда не сбрасываются: позиция слота — это {@code (int) (index &
 * mask)}. Благодаря этому нет отдельной обработки wrap-around и состояние «полон/пуст» выражается
 * простой разностью. {@code long} хватит на 2^63 операций — при 100 млн событий в секунду это
 * тысячи лет.
 *
 * <h2>Padding против false sharing</h2>
 *
 * <p>{@link PaddedLong} содержит 7 неиспользуемых {@code long} до значения и 7 после. В типичной
 * cache line 64 байта помещается 8 {@code long}; таким образом значение занимает отдельную линию, и
 * producer с consumer не инвалидируют кэш друг друга при каждой операции. Это заметно повышает
 * throughput на NUMA/многоядерных машинах, хотя и увеличивает потребление памяти на объект.
 *
 * <p>Поля намеренно не помечены {@code final}: JIT может переставлять/удалять неиспользуемые поля,
 * но на практике padding через поля работает и является классическим приёмом (Disruptor, Agrona).
 * Альтернатива — {@code @Contended}, требующая {@code -XX:-RestrictContended} и доступа к
 * внутреннему API.
 *
 * <h2>Память</h2>
 *
 * <p>Producer: запись в {@code slots[idx]} (обычная), затем volatile-запись {@code producerIndex}.
 * Consumer: volatile-чтение {@code producerIndex}, затем чтение слота. JMM гарантирует
 * happens-before: запись элемента видна consumer'у, если он увидел новый индекс.
 *
 * <p>Consumer после чтения зануляет слот до публикации {@code consumerIndex}. Это нужно по двум
 * причинам: (1) не держать ссылку на обработанный объект (GC), (2) producer не должен увидеть
 * освободившийся слот раньше, чем consumer закончил с ним работать — публикация {@code
 * consumerIndex} играет роль release-барьера.
 *
 * <h2>Почему не CAS</h2>
 *
 * <p>В SPSC каждый индекс пишет только один поток, поэтому CAS не нужен: достаточно
 * volatile-записи. Это принципиально дешевле: CAS — это атомарная RMW-операция с блокировкой cache
 * line, а volatile store — обычная запись с барьером.
 */
public final class Kata04_RingBufferSolution<T> extends Kata04_RingBuffer<T> {

  private final Object[] slots;
  private final int mask;
  private final PaddedLong producerIndex = new PaddedLong();
  private final PaddedLong consumerIndex = new PaddedLong();

  public Kata04_RingBufferSolution(int capacity) {
    super(capacity);
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be > 0, got " + capacity);
    }
    if (Integer.bitCount(capacity) != 1) {
      throw new IllegalArgumentException("capacity must be a power of two, got " + capacity);
    }
    this.slots = new Object[capacity];
    this.mask = capacity - 1;
  }

  @Override
  public boolean offer(T item) {
    Objects.requireNonNull(item, "item");

    long head = producerIndex.value; // пишет только этот поток — можно читать обычным
    long tail = consumerIndex.value; // volatile: видим, что consumer освободил слоты

    if (head - tail >= capacity) {
      return false;
    }

    slots[(int) (head & mask)] = item;
    producerIndex.value = head + 1; // volatile store = release: публикует элемент
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T poll() {
    long tail = consumerIndex.value;
    long head = producerIndex.value; // volatile: видим опубликованные элементы

    if (tail == head) {
      return null;
    }

    int slot = (int) (tail & mask);
    T item = (T) slots[slot];
    slots[slot] = null; // не держим ссылку на обработанный объект
    consumerIndex.value = tail + 1; // release: producer может переиспользовать слот
    return item;
  }

  @Override
  public int size() {
    long head = producerIndex.value;
    long tail = consumerIndex.value;
    long diff = head - tail;
    if (diff <= 0) {
      return 0;
    }
    return diff >= capacity ? capacity : (int) diff;
  }

  @Override
  public boolean isEmpty() {
    return producerIndex.value == consumerIndex.value;
  }

  /**
   * Значение с padding-полями по обе стороны. Поля не используются, но занимают байты в объекте и
   * не дают значению делить cache line с соседними данными.
   */
  private static final class PaddedLong {
    @SuppressWarnings("unused")
    private long p1, p2, p3, p4, p5, p6, p7;

    private volatile long value;

    @SuppressWarnings("unused")
    private long q1, q2, q3, q4, q5, q6, q7;
  }
}
