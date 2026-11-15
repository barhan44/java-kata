package dev.kata.solutions.concurrency;

import dev.kata.concurrency.Kata01_StripedLockCache;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Эталонное решение Kata 01.
 *
 * <h2>Выбранные структуры данных</h2>
 *
 * <ul>
 *   <li><b>Lock striping.</b> Кэш разбит на {@code stripeCount} сегментов, у каждого — собственный
 *       {@link ReentrantReadWriteLock} и обычный {@link HashMap}. Чтения параллельны внутри
 *       сегмента, записи сериализуются только по своему сегменту. Это классический приём из {@code
 *       ConcurrentHashMap} (до Java 8) и {@code Guava Striped}: при 64 полосах и 32 потоках
 *       вероятность коллизии по одной полосе мала.
 *   <li><b>HashMap, а не ConcurrentHashMap.</b> Внутри сегмента доступ уже защищён read-write lock;
 *       второй уровень конкурентности только добавил бы накладные расходы (CAS/volatile) и не дал
 *       бы ничего.
 *   <li><b>LongAdder для статистики.</b> Обычный {@code AtomicLong} на каждом чтении — это точка
 *       конкуренции на одной cache line; {@code LongAdder} распределяет счётчики по ячейкам и
 *       суммирует их только при чтении.
 *   <li><b>TTL в наносекундах.</b> {@link System#nanoTime()} монотонен и не зависит от перевода
 *       часов. Сравнение {@code now - expiresAt >= 0} корректно переживает переполнение {@code
 *       long} (разность двух значений nanoTime — это корректный интервал, пока он меньше 2^63 нс ≈
 *       292 года).
 * </ul>
 *
 * <h2>Защита от Cache Stampede</h2>
 *
 * <p>После промаха поток берёт <b>write-lock сегмента</b>, повторно проверяет запись и только затем
 * вызывает загрузчик. Пока загрузчик работает, читатели этого сегмента ждут. Это осознанный
 * компромисс:
 *
 * <ul>
 *   <li><b>Плюс:</b> простая и абсолютно надёжная гарантия «один загрузчик на ключ»; нет карты
 *       in-flight future, которую надо чистить при исключениях.
 *   <li><b>Минус:</b> медленный загрузчик блокирует весь сегмент, а не только один ключ. На
 *       практике сегментов много (64–256), а загрузчики быстрые (кэш в памяти).
 *   <li><b>Альтернатива для «тяжёлых» загрузчиков:</b> {@code ConcurrentHashMap<K,
 *       CompletableFuture<V>>} с {@code computeIfAbsent} — блокирует только ключ, но требует
 *       аккуратной обработки исключений и отмены, иначе future «залипнет» в карте.
 * </ul>
 *
 * <p>Пользовательский код под блокировкой — всегда риск (он может быть медленным или
 * реентерабельным). Поэтому в Javadoc заготовки явно оговорено: загрузчик должен быть быстрым и не
 * обращаться к этому же ключу повторно.
 *
 * <h2>Ленивая очистка</h2>
 *
 * <p>Фоновых потоков нет. Просроченная запись удаляется:
 *
 * <ul>
 *   <li>при чтении — мы всё равно обнаружили её и не хотим возвращать;
 *   <li>эпизодически при записи (каждая 64-я запись в сегмент чистит сегмент целиком), чтобы кэш не
 *       разрастался, если ключи перестали читать;
 *   <li>при вызове {@link #size()} — метод документирован как «не для горячего пути».
 * </ul>
 */
public final class Kata01_StripedLockCacheSolution<K, V> extends Kata01_StripedLockCache<K, V> {

  /** Наносекунд в миллисекунде. */
  private static final long NANOS_PER_MILLI = 1_000_000L;

  /** Как часто (в записях) запускать фоновую по отношению к put чистку сегмента. */
  private static final int PURGE_EVERY_N_PUTS = 64;

  private final Segment<K, V>[] segments;
  private final LongAdder hits = new LongAdder();
  private final LongAdder misses = new LongAdder();

  public Kata01_StripedLockCacheSolution(int stripeCount, long ttlMillis) {
    super(stripeCount, ttlMillis);
    if (stripeCount <= 0) {
      throw new IllegalArgumentException("stripeCount must be > 0, got " + stripeCount);
    }
    if (ttlMillis <= 0) {
      throw new IllegalArgumentException("ttlMillis must be > 0, got " + ttlMillis);
    }
    @SuppressWarnings("unchecked")
    Segment<K, V>[] created = (Segment<K, V>[]) new Segment<?, ?>[stripeCount];
    for (int i = 0; i < stripeCount; i++) {
      created[i] = new Segment<>();
    }
    this.segments = created;
  }

  @Override
  public V get(K key, Function<? super K, ? extends V> loader) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(loader, "loader");

    Segment<K, V> segment = segmentFor(key);
    long now = System.nanoTime();

    segment.readLock().lock();
    try {
      Entry<V> entry = segment.map.get(key);
      if (entry != null && !entry.isExpired(now)) {
        hits.increment();
        return entry.value;
      }
    } finally {
      segment.readLock().unlock();
    }

    // Промах: дальше нужен эксклюзивный доступ, чтобы ровно один поток вызвал loader.
    segment.writeLock().lock();
    try {
      now = System.nanoTime();
      Entry<V> entry = segment.map.get(key);
      if (entry != null) {
        if (!entry.isExpired(now)) {
          // Пока мы ждали write-lock, другой поток уже загрузил значение.
          // Промах уже посчитан — не превращаем его в hit и не искажаем статистику.
          return entry.value;
        }
        segment.map.remove(key);
      }

      misses.increment();
      V loaded = loader.apply(key);
      if (loaded == null) {
        throw new NullPointerException("loader returned null for key " + key);
      }
      segment.map.put(key, new Entry<>(loaded, expiryFromNow(now)));
      segment.maybePurge(now);
      return loaded;
    } finally {
      segment.writeLock().unlock();
    }
  }

  @Override
  public Optional<V> getIfPresent(K key) {
    Objects.requireNonNull(key, "key");

    Segment<K, V> segment = segmentFor(key);
    long now = System.nanoTime();

    segment.readLock().lock();
    try {
      Entry<V> entry = segment.map.get(key);
      if (entry != null && !entry.isExpired(now)) {
        hits.increment();
        return Optional.of(entry.value);
      }
    } finally {
      segment.readLock().unlock();
    }

    segment.writeLock().lock();
    try {
      now = System.nanoTime();
      Entry<V> entry = segment.map.get(key);
      if (entry != null && !entry.isExpired(now)) {
        // Значение успели обновить между read- и write-lock.
        hits.increment();
        return Optional.of(entry.value);
      }
      if (entry != null) {
        segment.map.remove(key);
      }
      misses.increment();
      return Optional.empty();
    } finally {
      segment.writeLock().unlock();
    }
  }

  @Override
  public void put(K key, V value) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");

    Segment<K, V> segment = segmentFor(key);
    long now = System.nanoTime();
    segment.writeLock().lock();
    try {
      segment.map.put(key, new Entry<>(value, expiryFromNow(now)));
      segment.maybePurge(now);
    } finally {
      segment.writeLock().unlock();
    }
  }

  @Override
  public boolean remove(K key) {
    Objects.requireNonNull(key, "key");

    Segment<K, V> segment = segmentFor(key);
    long now = System.nanoTime();
    segment.writeLock().lock();
    try {
      Entry<V> removed = segment.map.remove(key);
      return removed != null && !removed.isExpired(now);
    } finally {
      segment.writeLock().unlock();
    }
  }

  @Override
  public int size() {
    long now = System.nanoTime();
    int total = 0;
    for (Segment<K, V> segment : segments) {
      segment.writeLock().lock();
      try {
        segment.purgeExpired(now);
        total += segment.map.size();
      } finally {
        segment.writeLock().unlock();
      }
    }
    return total;
  }

  @Override
  public long hitCount() {
    return hits.sum();
  }

  @Override
  public long missCount() {
    return misses.sum();
  }

  @Override
  public void clear() {
    for (Segment<K, V> segment : segments) {
      segment.writeLock().lock();
      try {
        segment.map.clear();
        segment.putsSincePurge = 0;
      } finally {
        segment.writeLock().unlock();
      }
    }
  }

  private Segment<K, V> segmentFor(K key) {
    int hash = key.hashCode();
    // Spread: младшие биты hashCode часто плохо распределены (например, у Integer
    // это сам ключ, у строк — ASCII-коды). Перемешиваем старшие биты в младшие.
    hash ^= hash >>> 16;
    return segments[Math.floorMod(hash, stripeCount)];
  }

  /**
   * Вычисляет момент истечения TTL. При переполнении {@code now + ttlNanos} (теоретически возможно
   * при гигантском TTL) фиксируем максимум, чтобы сравнение не «сломалось».
   */
  private long expiryFromNow(long now) {
    long ttlNanos =
        ttlMillis > Long.MAX_VALUE / NANOS_PER_MILLI ? Long.MAX_VALUE : ttlMillis * NANOS_PER_MILLI;
    long expiresAt = now + ttlNanos;
    return expiresAt < now ? Long.MAX_VALUE : expiresAt;
  }

  /** Один сегмент: собственный lock, карта и счётчик записей для эпизодической чистки. */
  private static final class Segment<K, V> {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<K, Entry<V>> map = new HashMap<>();
    private int putsSincePurge;

    ReentrantReadWriteLock.ReadLock readLock() {
      return lock.readLock();
    }

    ReentrantReadWriteLock.WriteLock writeLock() {
      return lock.writeLock();
    }

    /** Вызывается только под write-lock. */
    void maybePurge(long now) {
      if (++putsSincePurge >= PURGE_EVERY_N_PUTS) {
        putsSincePurge = 0;
        purgeExpired(now);
      }
    }

    /** Вызывается только под write-lock. */
    void purgeExpired(long now) {
      map.values().removeIf(entry -> entry.isExpired(now));
    }
  }

  /** Иммутабельная запись: значение + абсолютный дедлайн в наносекундах. */
  private record Entry<V>(V value, long expiresAtNanos) {
    boolean isExpired(long now) {
      // Разность переживает переполнение nanoTime; см. Javadoc класса.
      return now - expiresAtNanos >= 0;
    }
  }
}
