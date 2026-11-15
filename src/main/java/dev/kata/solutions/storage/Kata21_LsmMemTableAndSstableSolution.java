package dev.kata.solutions.storage;

import dev.kata.storage.Kata21_LsmMemTableAndSstable;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Эталонное решение Kata 21.
 *
 * <h2>MemTable</h2>
 *
 * <p>Активная MemTable — {@link TreeMap} с компаратором беззнакового лексикографического сравнения
 * байтов. Ключи и значения копируются на входе, поэтому вызывающий может переиспользовать свои
 * массивы. Размер оценивается как {@code key.length + value.length + 16}: 16 байт — приблизительная
 * плата за запись в дереве/массивах. Порог проверяется после каждой записи; при превышении MemTable
 * целиком уходит в SSTable.
 *
 * <h2>Формат SSTable и Sparse Index</h2>
 *
 * <p>Запись: {@code [keyLen][key][valueLen][value]}; {@code valueLen = -1} — tombstone. Ключи идут
 * в возрастающем порядке. В памяти строится список anchor'ов: каждые {@code sparseIndexEvery}
 * записей сохраняются ключ и абсолютное смещение начала записи. Поиск:
 *
 * <ol>
 *   <li>бинарный поиск наибольшего anchor'а с ключом {@code <= target};
 *   <li>диапазон от этого anchor'а до следующего anchor'а (или конца файла);
 *   <li>линейное сканирование только этого блока.
 * </ol>
 *
 * <p>Благодаря сортировке можно остановиться, как только встретился ключ больше target: следующие
 * блоки начинаются с ещё больших ключей. В худшем случае сканируется {@code sparseIndexEvery + 1}
 * записей.
 *
 * <h2>Tombstone и несколько SSTable</h2>
 *
 * <p>{@code delete} пишет в MemTable запись с {@code value = null}. При поиске MemTable проверяется
 * первой. Если там tombstone — возвращается пустой Optional, и старые SSTable не читаются. Если
 * ключа в MemTable нет, SSTable перебираются от новых к старым; первая найденная запись (значение
 * или tombstone) определяет ответ. Именно это не даёт старому значению «воскреснуть» после
 * удаления.
 *
 * <h2>Persistence</h2>
 *
 * <p>При открытии каталога файлы {@code sstable-XXXXXXXX.sst} сортируются по id и загружаются от
 * старых к новым; разреженный индекс перестраивается чтением ключей (значения не читаются).
 * Активная MemTable при аварийном завершении теряется — в production её защищает WAL. При чистом
 * {@link #close()} MemTable сбрасывается в SSTable.
 *
 * <h2>Потокобезопасность</h2>
 *
 * <p>Один {@link ReentrantLock} защищает MemTable, список SSTable и операции flush. Чтение и запись
 * сериализуются; это простой и корректный вариант для учебного движка. Production-системы обычно
 * используют отдельные immutable MemTable, очередь flush и read path без блокировки.
 */
public final class Kata21_LsmMemTableAndSstableSolution extends Kata21_LsmMemTableAndSstable {

  private static final int ENTRY_OVERHEAD = 16;
  private static final String SSTABLE_PREFIX = "sstable-";
  private static final String SSTABLE_SUFFIX = ".sst";

  private static final Comparator<byte[]> KEY_COMPARATOR =
      (left, right) -> {
        int limit = Math.min(left.length, right.length);
        for (int i = 0; i < limit; i++) {
          int a = left[i] & 0xFF;
          int b = right[i] & 0xFF;
          if (a != b) {
            return a - b;
          }
        }
        return left.length - right.length;
      };

  private final ReentrantLock lock = new ReentrantLock();
  private final TreeMap<byte[], Entry> memTable = new TreeMap<>(KEY_COMPARATOR);
  private final List<Sstable> sstables = new ArrayList<>(); // newest first

  private long memTableBytes;
  private long nextSstableId = 1L;
  private boolean closed;

  public Kata21_LsmMemTableAndSstableSolution(Path directory, long memTableMaxBytes) {
    this(directory, memTableMaxBytes, 16);
  }

  public Kata21_LsmMemTableAndSstableSolution(
      Path directory, long memTableMaxBytes, int sparseIndexEvery) {
    super(directory, memTableMaxBytes, sparseIndexEvery);
    Objects.requireNonNull(directory, "directory");
    if (memTableMaxBytes <= 0L) {
      throw new IllegalArgumentException("memTableMaxBytes must be > 0, got " + memTableMaxBytes);
    }
    if (sparseIndexEvery <= 0) {
      throw new IllegalArgumentException("sparseIndexEvery must be > 0, got " + sparseIndexEvery);
    }

    try {
      Files.createDirectories(directory);
      List<Path> files;
      try (Stream<Path> stream = Files.list(directory)) {
        files =
            stream.filter(Kata21_LsmMemTableAndSstableSolution::isSstableFile).sorted().toList();
      }
      for (Path file : files) {
        long id = parseSstableId(file);
        Sstable sstable = Sstable.open(file, sparseIndexEvery);
        sstables.add(0, sstable); // старые -> новые; в списке новые первыми
        nextSstableId = Math.max(nextSstableId, id + 1L);
      }
    } catch (IOException e) {
      closeLoaded();
      throw new UncheckedIOException("cannot open LSM directory " + directory, e);
    } catch (RuntimeException e) {
      closeLoaded();
      throw e;
    }
  }

  @Override
  public void put(byte[] key, byte[] value) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
    byte[] keyCopy = key.clone();
    byte[] valueCopy = value.clone();

    lock.lock();
    try {
      ensureOpen();
      putLocked(keyCopy, new Entry(valueCopy, false));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void delete(byte[] key) {
    Objects.requireNonNull(key, "key");
    byte[] keyCopy = key.clone();

    lock.lock();
    try {
      ensureOpen();
      putLocked(keyCopy, new Entry(null, true));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Optional<byte[]> get(byte[] key) {
    Objects.requireNonNull(key, "key");

    lock.lock();
    try {
      ensureOpen();

      Entry entry = memTable.get(key);
      if (entry != null) {
        return entry.tombstone() ? Optional.empty() : Optional.of(entry.value().clone());
      }

      for (Sstable sstable : sstables) {
        Sstable.Lookup lookup = sstable.get(key);
        if (lookup.status() == Sstable.Status.VALUE) {
          return Optional.of(lookup.value());
        }
        if (lookup.status() == Sstable.Status.TOMBSTONE) {
          // Tombstone в более новой таблице перекрывает любые старые значения.
          return Optional.empty();
        }
      }
      return Optional.empty();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void flush() {
    lock.lock();
    try {
      ensureOpen();
      flushLocked();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public int sstableCount() {
    lock.lock();
    try {
      ensureOpen();
      return sstables.size();
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
      try {
        flushLocked();
      } finally {
        closed = true;
        closeLoaded();
      }
    } finally {
      lock.unlock();
    }
  }

  private void putLocked(byte[] key, Entry newEntry) {
    Entry old = memTable.put(key, newEntry);
    long oldBytes = old == null ? 0L : entryBytes(key, old.value());
    memTableBytes += entryBytes(key, newEntry.value()) - oldBytes;
    if (memTableBytes > memTableMaxBytes) {
      flushLocked();
    }
  }

  private void flushLocked() {
    if (memTable.isEmpty()) {
      return;
    }
    long id = nextSstableId;
    Sstable sstable = Sstable.write(directory, id, memTable, sparseIndexEvery);
    nextSstableId = id + 1L;
    sstables.add(0, sstable);
    memTable.clear();
    memTableBytes = 0L;
  }

  private void closeLoaded() {
    for (Sstable sstable : sstables) {
      sstable.close();
    }
    sstables.clear();
  }

  private static long entryBytes(byte[] key, byte[] value) {
    return ENTRY_OVERHEAD + key.length + (value == null ? 0L : value.length);
  }

  private static boolean isSstableFile(Path path) {
    String name = path.getFileName().toString();
    if (!name.startsWith(SSTABLE_PREFIX) || !name.endsWith(SSTABLE_SUFFIX)) {
      return false;
    }
    try {
      Long.parseLong(
          name.substring(SSTABLE_PREFIX.length(), name.length() - SSTABLE_SUFFIX.length()));
      return true;
    } catch (NumberFormatException invalid) {
      return false;
    }
  }

  private static long parseSstableId(Path path) {
    String name = path.getFileName().toString();
    return Long.parseLong(
        name.substring(SSTABLE_PREFIX.length(), name.length() - SSTABLE_SUFFIX.length()));
  }

  private static String sstableName(long id) {
    return String.format("%s%08d%s", SSTABLE_PREFIX, id, SSTABLE_SUFFIX);
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("LSM is closed");
    }
  }

  /** Ссылка на значение либо tombstone. */
  private record Entry(byte[] value, boolean tombstone) {}

  /** Одна immutable SSTable с разреженным индексом. */
  private static final class Sstable {

    private enum Status {
      MISSING,
      TOMBSTONE,
      VALUE
    }

    private record Lookup(Status status, byte[] value) {
      static final Lookup MISSING = new Lookup(Status.MISSING, null);
      static final Lookup TOMBSTONE = new Lookup(Status.TOMBSTONE, null);

      static Lookup ofValue(byte[] value) {
        return new Lookup(Status.VALUE, value);
      }
    }

    private record Anchor(byte[] key, long offset) {}

    private final Path path;
    private final FileChannel channel;
    private final List<Anchor> anchors;
    private final long fileSize;

    private Sstable(Path path, FileChannel channel, List<Anchor> anchors, long fileSize) {
      this.path = path;
      this.channel = channel;
      this.anchors = anchors;
      this.fileSize = fileSize;
    }

    static Sstable write(
        Path directory, long id, TreeMap<byte[], Entry> table, int sparseIndexEvery) {
      Path file = directory.resolve(sstableName(id));
      FileChannel channel = null;
      try {
        channel =
            FileChannel.open(
                file,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);
        long position = 0L;
        int recordIndex = 0;
        List<Anchor> anchors = new ArrayList<>();

        for (var mapEntry : table.entrySet()) {
          byte[] key = mapEntry.getKey();
          Entry entry = mapEntry.getValue();
          byte[] value = entry.value();
          int valueLength = entry.tombstone() ? -1 : value.length;

          if (recordIndex % sparseIndexEvery == 0) {
            anchors.add(new Anchor(key.clone(), position));
          }
          int recordSize = 2 * Integer.BYTES + key.length + (valueLength < 0 ? 0 : valueLength);
          ByteBuffer buffer = ByteBuffer.allocate(recordSize);
          buffer.putInt(key.length);
          buffer.put(key);
          buffer.putInt(valueLength);
          if (valueLength >= 0) {
            buffer.put(value);
          }
          buffer.flip();
          writeFully(channel, buffer, position);
          position += recordSize;
          recordIndex++;
        }

        channel.force(false);
        return new Sstable(file, channel, List.copyOf(anchors), position);
      } catch (IOException e) {
        closeQuietly(channel);
        try {
          Files.deleteIfExists(file);
        } catch (IOException ignored) {
          // Исходная ошибка важнее ошибки очистки.
        }
        throw new UncheckedIOException("cannot write SSTable " + file, e);
      }
    }

    static Sstable open(Path file, int sparseIndexEvery) {
      FileChannel channel = null;
      try {
        channel = FileChannel.open(file, StandardOpenOption.READ);
        long size = channel.size();
        long position = 0L;
        int recordIndex = 0;
        List<Anchor> anchors = new ArrayList<>();

        while (position < size) {
          long recordStart = position;
          int keyLength = readInt(channel, position);
          position += Integer.BYTES;
          if (keyLength < 0 || position + keyLength > size) {
            throw new IOException("corrupt key length at " + recordStart + " in " + file);
          }
          byte[] key = readBytes(channel, position, keyLength);
          position += keyLength;

          int valueLength = readInt(channel, position);
          position += Integer.BYTES;
          if (valueLength < -1 || (valueLength >= 0 && position + valueLength > size)) {
            throw new IOException("corrupt value length at " + recordStart + " in " + file);
          }

          if (recordIndex % sparseIndexEvery == 0) {
            anchors.add(new Anchor(key, recordStart));
          }
          if (valueLength >= 0) {
            position += valueLength;
          }
          recordIndex++;
        }

        return new Sstable(file, channel, List.copyOf(anchors), size);
      } catch (IOException e) {
        closeQuietly(channel);
        throw new UncheckedIOException("cannot open SSTable " + file, e);
      }
    }

    Lookup get(byte[] target) {
      try {
        if (anchors.isEmpty()) {
          return Lookup.MISSING;
        }

        int low = 0;
        int high = anchors.size() - 1;
        int selected = -1;
        while (low <= high) {
          int middle = (low + high) >>> 1;
          int comparison = KEY_COMPARATOR.compare(anchors.get(middle).key(), target);
          if (comparison <= 0) {
            selected = middle;
            low = middle + 1;
          } else {
            high = middle - 1;
          }
        }

        long start = selected < 0 ? 0L : anchors.get(selected).offset();
        long end = selected + 1 < anchors.size() ? anchors.get(selected + 1).offset() : fileSize;

        long position = start;
        while (position < end) {
          int keyLength = readInt(channel, position);
          position += Integer.BYTES;
          if (keyLength < 0 || position + keyLength + Integer.BYTES > end) {
            return Lookup.MISSING;
          }
          byte[] key = readBytes(channel, position, keyLength);
          position += keyLength;

          int valueLength = readInt(channel, position);
          position += Integer.BYTES;
          if (valueLength < -1) {
            return Lookup.MISSING;
          }
          int comparison = KEY_COMPARATOR.compare(key, target);
          if (comparison == 0) {
            if (valueLength == -1) {
              return Lookup.TOMBSTONE;
            }
            if (position + valueLength > end) {
              return Lookup.MISSING;
            }
            return Lookup.ofValue(readBytes(channel, position, valueLength));
          }
          if (comparison > 0) {
            return Lookup.MISSING;
          }
          if (valueLength >= 0) {
            position += valueLength;
          }
        }
        return Lookup.MISSING;
      } catch (IOException e) {
        throw new UncheckedIOException("cannot read SSTable " + path, e);
      }
    }

    void close() {
      closeQuietly(channel);
    }

    private static void closeQuietly(FileChannel channel) {
      if (channel != null) {
        try {
          channel.close();
        } catch (IOException ignored) {
          // Закрытие-best effort.
        }
      }
    }

    private static int readInt(FileChannel channel, long position) throws IOException {
      ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
      readFully(channel, buffer, position);
      buffer.flip();
      return buffer.getInt();
    }

    private static byte[] readBytes(FileChannel channel, long position, int length)
        throws IOException {
      if (length == 0) {
        return new byte[0];
      }
      ByteBuffer buffer = ByteBuffer.allocate(length);
      readFully(channel, buffer, position);
      return buffer.array();
    }

    private static void readFully(FileChannel channel, ByteBuffer buffer, long position)
        throws IOException {
      long offset = position;
      while (buffer.hasRemaining()) {
        int read = channel.read(buffer, offset);
        if (read < 0) {
          throw new EOFException("unexpected EOF");
        }
        if (read == 0) {
          Thread.onSpinWait();
          continue;
        }
        offset += read;
      }
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer, long position)
        throws IOException {
      long offset = position;
      while (buffer.hasRemaining()) {
        int written = channel.write(buffer, offset);
        if (written == 0) {
          Thread.onSpinWait();
          continue;
        }
        offset += written;
      }
    }
  }
}
