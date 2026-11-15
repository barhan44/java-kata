package dev.kata.solutions.storage;

import dev.kata.storage.Kata20_WriteAheadLog;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.CRC32;

/**
 * Эталонное решение Kata 20.
 *
 * <h2>Формат и контрольная сумма</h2>
 *
 * <p>Кадр пишется одним {@link ByteBuffer}: {@code [CRC32][LSN][length][payload]}. CRC считается по
 * 8 байтам LSN, 4 байтам длины и payload — то есть по всему, кроме самого поля CRC. Это позволяет
 * обнаружить как повреждение данных, так и подмену LSN/длины. Все числа big-endian, поэтому формат
 * одинаков на любой платформе.
 *
 * <p>Порядок записи: сначала формируется весь кадр в памяти, затем {@code writeFully} с абсолютной
 * позицией. Запись одного кадра не перемешивается с другими: {@link ReentrantLock} сериализует
 * {@code append}. После записи вызывается {@code FileChannel.force(false)} — данные уходят на диск
 * до возврата LSN. Метаданные (например, размер файла) при {@code force(false)} не гарантируются,
 * но это ровно тот контракт, который запрошен и который использует большинство WAL.
 *
 * <h2>Восстановление</h2>
 *
 * <p>{@code scanLocked()} читает файл позиционно. Для каждого кадра:
 *
 * <ol>
 *   <li>проверяет, что заголовок (16 байт) целиком помещается в файл;
 *   <li>проверяет {@code length >= 0}, что payload целиком помещается в файл, и что LSN строго
 *       возрастает;
 *   <li>пересчитывает CRC и сравнивает с сохранённой;
 *   <li>только после всех проверок добавляет запись.
 * </ol>
 *
 * <p>Любая неудача означает torn write или порчу хвоста: сканирование останавливается, но уже
 * прочитанные записи возвращаются. Исключение не бросается — это главное требование к recovery.
 * {@code validLength} запоминает конец последней валидной записи; {@code nextLsn} — следующий
 * свободный LSN. Если файл содержит мусор после {@code validLength}, первый {@code append} делает
 * {@code truncate(validLength)} и пишет поверх повреждённого хвоста.
 *
 * <h2>Prefix compaction</h2>
 *
 * <p>{@link #truncatePrefix(long)} перечитывает валидные записи, обнуляет файл и переписывает
 * только записи с {@code LSN >= upToLsn}, сохраняя исходные LSN. Счётчик {@code nextLsn} не
 * уменьшается: LSN — это монотонный идентификатор, и после очистки журнала следующий append должен
 * получить новый номер, а не старый. Операция дорогая (O(размера журнала)), но выполняется редко —
 * при сегментной ротации или checkpoint.
 *
 * <h2>Потокобезопасность</h2>
 *
 * <p>Один {@link ReentrantLock} защищает канал, {@code nextLsn}, {@code validLength} и {@link
 * CRC32} (экземпляр CRC32 не потокобезопасен). {@code recover}, {@code append}, {@code
 * truncatePrefix}, {@code force} и {@code close} сериализуются. Один WAL — один писатель;
 * конкурентные append допустимы, но упорядочиваются блокировкой.
 */
public final class Kata20_WriteAheadLogSolution extends Kata20_WriteAheadLog {

  private static final int HEADER_SIZE = Integer.BYTES + Long.BYTES + Integer.BYTES;
  private static final int META_SIZE = Long.BYTES + Integer.BYTES;

  private final ReentrantLock lock = new ReentrantLock();
  private final CRC32 crc = new CRC32();
  private final FileChannel channel;

  private long nextLsn = 1L;
  private long validLength;
  private boolean closed;

  public Kata20_WriteAheadLogSolution(Path path) {
    super(path);
    try {
      Path parent = path.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      this.channel =
          FileChannel.open(
              path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot open WAL " + path, e);
    }

    lock.lock();
    try {
      // При открытии нужны только validLength/nextLsn; не материализуем весь журнал.
      scanLocked(false);
    } catch (IOException e) {
      try {
        channel.close();
      } catch (IOException ignored) {
        // Исходная ошибка важнее ошибки закрытия.
      }
      throw new UncheckedIOException("cannot scan WAL " + path, e);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long append(byte[] payload) {
    Objects.requireNonNull(payload, "payload");
    byte[] data = payload.clone();

    lock.lock();
    try {
      ensureOpen();
      if (channel.size() > validLength) {
        // Хвост после последней валидной записи — мусор от torn write. Убираем его,
        // иначе новая запись окажется за ним и recovery её не увидит.
        channel.truncate(validLength);
      }

      long lsn = nextLsn;
      long position = validLength;
      writeFrame(lsn, data, position);
      validLength = position + HEADER_SIZE + data.length;
      nextLsn = lsn + 1L;
      channel.force(false);
      return lsn;
    } catch (IOException e) {
      throw new UncheckedIOException("append failed", e);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public List<LogEntry> recover() {
    lock.lock();
    try {
      ensureOpen();
      return scanLocked(true);
    } catch (IOException e) {
      throw new UncheckedIOException("recover failed", e);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void truncatePrefix(long upToLsn) {
    lock.lock();
    try {
      ensureOpen();
      if (upToLsn <= 1L) {
        return;
      }

      List<LogEntry> entries = scanLocked(true);
      long position = 0L;
      channel.truncate(0L);
      for (LogEntry entry : entries) {
        if (entry.lsn() >= upToLsn) {
          byte[] data = entry.payload();
          writeFrame(entry.lsn(), data, position);
          position += HEADER_SIZE + data.length;
        }
      }
      validLength = position;
      // nextLsn после scanLocked равен max(valid LSN)+1 и намеренно не уменьшается.
      channel.force(false);
    } catch (IOException e) {
      throw new UncheckedIOException("truncatePrefix failed", e);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void force() {
    lock.lock();
    try {
      ensureOpen();
      channel.force(false);
    } catch (IOException e) {
      throw new UncheckedIOException("force failed", e);
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
      closed = true;
      try {
        channel.force(false);
      } finally {
        channel.close();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("close failed", e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Сканирует журнал и обновляет {@code validLength}/{@code nextLsn}.
   *
   * <p>Позиционные чтения не меняют позицию канала. При любой невалидности цикл прерывается, а
   * {@code validLength} остаётся на конце последней хорошей записи.
   */
  private List<LogEntry> scanLocked(boolean collect) throws IOException {
    long size = channel.size();
    long position = 0L;
    long lastLsn = 0L;
    List<LogEntry> entries = collect ? new ArrayList<>() : List.of();
    ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);

    while (position + HEADER_SIZE <= size) {
      header.clear();
      if (!readFully(header, position)) {
        break;
      }
      header.flip();
      int storedCrc = header.getInt();
      long lsn = header.getLong();
      int length = header.getInt();

      if (lsn <= lastLsn || length < 0 || length > size - position - HEADER_SIZE) {
        break;
      }

      ByteBuffer payloadBuffer = ByteBuffer.allocate(length);
      if (!readFully(payloadBuffer, position + HEADER_SIZE)) {
        break;
      }
      byte[] payload = payloadBuffer.array();
      if (computeCrc(lsn, length, payload) != Integer.toUnsignedLong(storedCrc)) {
        break;
      }

      if (collect) {
        entries.add(new LogEntry(lsn, payload));
      }
      lastLsn = lsn;
      position += HEADER_SIZE + length;
    }

    validLength = position;
    // LSN не должен откатываться: после truncatePrefix файл может быть пуст, но счётчик
    // уже выдавал номера. max() сохраняет монотонность в пределах жизни экземпляра.
    nextLsn = Math.max(nextLsn, lastLsn + 1L);
    return entries;
  }

  private void writeFrame(long lsn, byte[] payload, long position) throws IOException {
    int length = payload.length;
    byte[] meta = new byte[META_SIZE];
    ByteBuffer metaBuffer = ByteBuffer.wrap(meta);
    metaBuffer.putLong(lsn).putInt(length);

    crc.reset();
    crc.update(meta, 0, META_SIZE);
    crc.update(payload, 0, length);
    int checksum = (int) crc.getValue();

    ByteBuffer frame = ByteBuffer.allocate(HEADER_SIZE + length);
    frame.putInt(checksum);
    frame.put(meta);
    frame.put(payload);
    frame.flip();
    writeFully(frame, position);
  }

  private void writeFully(ByteBuffer buffer, long position) throws IOException {
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

  private boolean readFully(ByteBuffer buffer, long position) throws IOException {
    long offset = position;
    while (buffer.hasRemaining()) {
      int read = channel.read(buffer, offset);
      if (read < 0) {
        return false;
      }
      if (read == 0) {
        Thread.onSpinWait();
        continue;
      }
      offset += read;
    }
    return true;
  }

  private long computeCrc(long lsn, int length, byte[] payload) {
    byte[] meta = new byte[META_SIZE];
    ByteBuffer buffer = ByteBuffer.wrap(meta);
    buffer.putLong(lsn).putInt(length);
    crc.reset();
    crc.update(meta, 0, META_SIZE);
    crc.update(payload, 0, length);
    return crc.getValue();
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("WAL is closed");
    }
  }
}
