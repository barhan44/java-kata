package dev.kata.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.storage.Kata20_WriteAheadLogSolution;
import dev.kata.storage.Kata20_WriteAheadLog.LogEntry;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Тесты Kata 20.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata20* -DuseSolution=true</pre>
 */
@DisplayName("Kata 20: Write-Ahead Log с CRC32")
class Kata20_WriteAheadLogTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final int FRAME_OVERHEAD = 16;

  @TempDir Path tempDir;

  private Kata20_WriteAheadLog newWal(Path file) {
    return USE_SOLUTION ? new Kata20_WriteAheadLogSolution(file) : new Kata20_WriteAheadLog(file);
  }

  @Test
  @DisplayName("append/recover: LSN монотонны, payload читается после рестарта")
  void appendAndRecoverAcrossRestart() {
    Path file = tempDir.resolve("wal.log");

    try (Kata20_WriteAheadLog wal = newWal(file)) {
      assertThat(wal.append(bytes("one"))).isEqualTo(1L);
      assertThat(wal.append(bytes("two"))).isEqualTo(2L);
      assertThat(wal.append(bytes("three"))).isEqualTo(3L);

      assertThat(wal.recover())
          .containsExactly(
              new LogEntry(1L, bytes("one")),
              new LogEntry(2L, bytes("two")),
              new LogEntry(3L, bytes("three")));
    }

    try (Kata20_WriteAheadLog wal = newWal(file)) {
      assertThat(wal.recover()).hasSize(3);
      assertThat(wal.append(bytes("four"))).isEqualTo(4L);
    }
  }

  @Test
  @DisplayName("Пустой файл: recover пуст, первый LSN = 1; пустой payload допустим")
  void emptyLogAndEmptyPayload() {
    Path file = tempDir.resolve("empty.log");

    try (Kata20_WriteAheadLog wal = newWal(file)) {
      assertThat(wal.recover()).isEmpty();
      assertThat(wal.append(new byte[0])).isEqualTo(1L);
      assertThat(wal.append(bytes("x"))).isEqualTo(2L);
      assertThat(wal.recover().get(0).payload()).isEmpty();
    }
  }

  @Test
  @DisplayName("Torn write: обрезанный хвост отбрасывается, append продолжает LSN")
  void tornTailIsDiscarded() throws IOException {
    Path file = tempDir.resolve("torn.log");
    try (Kata20_WriteAheadLog wal = newWal(file)) {
      for (int i = 0; i < 5; i++) {
        wal.append(bytes("record-" + i));
      }
    }

    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
      channel.truncate(channel.size() - 7);
    }

    try (Kata20_WriteAheadLog wal = newWal(file)) {
      assertThat(wal.recover()).hasSize(4);
      assertThat(wal.append(bytes("after-torn"))).isEqualTo(5L);
      assertThat(wal.recover()).hasSize(5);
      assertThat(wal.recover().get(4).payload()).isEqualTo(bytes("after-torn"));
    }
  }

  @Test
  @DisplayName("Битый CRC в последней записи: recovery останавливается на предыдущей")
  void corruptCrcInTail() throws IOException {
    Path file = tempDir.resolve("crc.log");
    try (Kata20_WriteAheadLog wal = newWal(file)) {
      for (int i = 0; i < 4; i++) {
        wal.append(bytes("payload-" + i));
      }
    }

    flipByte(file, Files.size(file) - 1);

    try (Kata20_WriteAheadLog wal = newWal(file)) {
      assertThat(wal.recover()).hasSize(3);
      assertThat(wal.append(bytes("recovered"))).isEqualTo(4L);
      assertThat(wal.recover())
          .containsExactly(
              new LogEntry(1L, bytes("payload-0")),
              new LogEntry(2L, bytes("payload-1")),
              new LogEntry(3L, bytes("payload-2")),
              new LogEntry(4L, bytes("recovered")));
    }
  }

  @Test
  @DisplayName("Порча в середине хвоста: записи после неё отбрасываются")
  void corruptionInMiddleOfTailStopsRecovery() throws IOException {
    Path file = tempDir.resolve("middle.log");
    try (Kata20_WriteAheadLog wal = newWal(file)) {
      for (int i = 0; i < 5; i++) {
        wal.append(bytes("rec" + i));
      }
    }

    long frameSize = FRAME_OVERHEAD + 4;
    long thirdPayloadStart = 2 * frameSize + FRAME_OVERHEAD;
    flipByte(file, thirdPayloadStart);

    try (Kata20_WriteAheadLog wal = newWal(file)) {
      assertThat(wal.recover())
          .containsExactly(new LogEntry(1L, bytes("rec0")), new LogEntry(2L, bytes("rec1")));
      assertThat(wal.append(bytes("new3"))).isEqualTo(3L);
      assertThat(wal.recover()).hasSize(3);
    }
  }

  @Test
  @DisplayName("Мусор без валидного заголовка: журнал пуст, append начинает с LSN 1")
  void garbageFileIsTreatedAsEmpty() throws IOException {
    Path file = tempDir.resolve("garbage.log");
    Files.write(file, bytes("this is not a WAL frame"));

    try (Kata20_WriteAheadLog wal = newWal(file)) {
      assertThat(wal.recover()).isEmpty();
      assertThat(wal.append(bytes("fresh"))).isEqualTo(1L);
      assertThat(wal.recover()).containsExactly(new LogEntry(1L, bytes("fresh")));
    }
  }

  @Test
  @DisplayName("truncatePrefix сохраняет LSN и не откатывает счётчик")
  void truncatePrefix() {
    Path file = tempDir.resolve("truncate.log");
    try (Kata20_WriteAheadLog wal = newWal(file)) {
      for (int i = 1; i <= 5; i++) {
        wal.append(bytes("v" + i));
      }

      wal.truncatePrefix(3L);
      assertThat(wal.recover())
          .containsExactly(
              new LogEntry(3L, bytes("v3")),
              new LogEntry(4L, bytes("v4")),
              new LogEntry(5L, bytes("v5")));
      assertThat(wal.append(bytes("v6"))).isEqualTo(6L);

      wal.truncatePrefix(100L);
      assertThat(wal.recover()).isEmpty();
      assertThat(wal.append(bytes("v7"))).isEqualTo(7L);
    }
  }

  @Test
  @DisplayName("truncatePrefix(<=1) — no-op")
  void truncatePrefixNoOp() {
    Path file = tempDir.resolve("noop.log");
    try (Kata20_WriteAheadLog wal = newWal(file)) {
      wal.append(bytes("a"));
      wal.append(bytes("b"));

      wal.truncatePrefix(1L);
      wal.truncatePrefix(0L);

      assertThat(wal.recover()).hasSize(2);
    }
  }

  @Test
  @DisplayName("Payload и LogEntry защищены от внешней мутации")
  void defensiveCopies() {
    Path file = tempDir.resolve("copies.log");
    byte[] payload = bytes("original");
    try (Kata20_WriteAheadLog wal = newWal(file)) {
      wal.append(payload);
      payload[0] = 'X';

      byte[] recovered = wal.recover().get(0).payload();
      assertThat(recovered).isEqualTo(bytes("original"));
      recovered[0] = 'Y';
      assertThat(wal.recover().get(0).payload()).isEqualTo(bytes("original"));
    }
  }

  @Test
  @DisplayName("force не меняет данные; close идемпотентен, после close операции запрещены")
  void forceAndClose() {
    Path file = tempDir.resolve("close.log");
    Kata20_WriteAheadLog wal = newWal(file);
    wal.append(bytes("a"));
    wal.force();
    wal.close();
    wal.close();

    assertThatThrownBy(() -> wal.append(bytes("b"))).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(wal::recover).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> wal.truncatePrefix(2L)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(wal::force).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("null payload и null path запрещены")
  void nullsRejected() {
    assertThatThrownBy(() -> newWal(null)).isInstanceOf(NullPointerException.class);
    try (Kata20_WriteAheadLog wal = newWal(tempDir.resolve("null.log"))) {
      assertThatThrownBy(() -> wal.append(null)).isInstanceOf(NullPointerException.class);
    }
  }

  @Test
  @DisplayName("Вложенные каталоги создаются автоматически")
  void createsParentDirectories() {
    Path file = tempDir.resolve("a/b/c/wal.log");
    try (Kata20_WriteAheadLog wal = newWal(file)) {
      wal.append(bytes("nested"));
      assertThat(wal.recover()).containsExactly(new LogEntry(1L, bytes("nested")));
    }
    assertThat(Files.exists(file)).isTrue();
  }

  @Test
  @DisplayName("Большой payload записывается и восстанавливается")
  void largePayload() {
    byte[] payload = new byte[1024 * 1024];
    for (int i = 0; i < payload.length; i++) {
      payload[i] = (byte) (i * 31);
    }
    Path file = tempDir.resolve("large.log");

    try (Kata20_WriteAheadLog wal = newWal(file)) {
      assertThat(wal.append(payload)).isEqualTo(1L);
      List<LogEntry> entries = wal.recover();
      assertThat(entries).hasSize(1);
      assertThat(entries.get(0).payload()).isEqualTo(payload);
    }
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static void flipByte(Path file, long position) throws IOException {
    try (FileChannel channel =
        FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.allocate(1);
      channel.read(buffer, position);
      buffer.flip();
      byte value = buffer.get();
      buffer.clear();
      buffer.put((byte) (value ^ 0xFF));
      buffer.flip();
      channel.write(buffer, position);
    }
  }
}
