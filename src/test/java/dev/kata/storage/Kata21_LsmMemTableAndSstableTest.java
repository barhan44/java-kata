package dev.kata.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.storage.Kata21_LsmMemTableAndSstableSolution;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Тесты Kata 21.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata21* -DuseSolution=true</pre>
 */
@DisplayName("Kata 21: LSM MemTable + SSTable")
class Kata21_LsmMemTableAndSstableTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final long LARGE_MEMTABLE = 10_000_000L;

  @TempDir Path tempDir;

  private Kata21_LsmMemTableAndSstable newLsm(
      Path directory, long memTableMaxBytes, int sparseIndexEvery) {
    return USE_SOLUTION
        ? new Kata21_LsmMemTableAndSstableSolution(directory, memTableMaxBytes, sparseIndexEvery)
        : new Kata21_LsmMemTableAndSstable(directory, memTableMaxBytes, sparseIndexEvery);
  }

  @Test
  @DisplayName("put/get и перезапись в активной MemTable")
  void putGetAndOverwrite() {
    try (Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("basic"), LARGE_MEMTABLE, 4)) {
      lsm.put(bytes("a"), bytes("one"));
      assertThat(value(lsm.get(bytes("a")))).isEqualTo("one");

      lsm.put(bytes("a"), bytes("two"));
      assertThat(value(lsm.get(bytes("a")))).isEqualTo("two");
      assertThat(lsm.get(bytes("missing"))).isEmpty();
    }
  }

  @Test
  @DisplayName("Tombstone не даёт старому значению воскреснуть из SSTable")
  void tombstoneShadowsOlderSstableValue() {
    try (Kata21_LsmMemTableAndSstable lsm =
        newLsm(tempDir.resolve("tombstone"), LARGE_MEMTABLE, 4)) {
      lsm.put(bytes("key"), bytes("old-value"));
      lsm.flush();
      assertThat(value(lsm.get(bytes("key")))).isEqualTo("old-value");

      lsm.delete(bytes("key"));
      assertThat(lsm.get(bytes("key"))).isEmpty();

      lsm.flush();
      assertThat(lsm.get(bytes("key"))).isEmpty();

      lsm.put(bytes("key"), bytes("new-value"));
      assertThat(value(lsm.get(bytes("key")))).isEqualTo("new-value");
      lsm.flush();
      assertThat(value(lsm.get(bytes("key")))).isEqualTo("new-value");
    }
  }

  @Test
  @DisplayName("Поиск по нескольким SSTable: новое значение перекрывает старое")
  void readsAcrossMultipleSstables() {
    try (Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("multi"), LARGE_MEMTABLE, 4)) {
      lsm.put(bytes("key"), bytes("v1"));
      lsm.flush();
      lsm.put(bytes("key"), bytes("v2"));
      lsm.flush();
      lsm.put(bytes("key"), bytes("v3"));

      assertThat(value(lsm.get(bytes("key")))).isEqualTo("v3");
      lsm.flush();
      assertThat(value(lsm.get(bytes("key")))).isEqualTo("v3");
      assertThat(lsm.sstableCount()).isGreaterThanOrEqualTo(3);

      lsm.delete(bytes("key"));
      lsm.flush();
      assertThat(lsm.get(bytes("key"))).isEmpty();
    }
  }

  @Test
  @DisplayName("Sparse Index находит все ключи в блоке и на границах блоков")
  void sparseIndexFindsEveryKey() {
    int keyCount = 100;
    try (Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("sparse"), LARGE_MEMTABLE, 4)) {
      for (int i = 0; i < keyCount; i++) {
        String key = String.format("key-%03d", i);
        lsm.put(bytes(key), bytes(key + "-value"));
      }
      assertThat(lsm.sstableCount()).isZero();
      lsm.flush();
      assertThat(lsm.sstableCount()).isEqualTo(1);

      for (int i = 0; i < keyCount; i++) {
        String key = String.format("key-%03d", i);
        assertThat(value(lsm.get(bytes(key)))).isEqualTo(key + "-value");
      }

      assertThat(lsm.get(bytes("aaa"))).isEmpty();
      assertThat(lsm.get(bytes("key-050x"))).isEmpty();
      assertThat(lsm.get(bytes("zzz"))).isEmpty();
    }
  }

  @Test
  @DisplayName("Автоматический flush при превышении memTableMaxBytes")
  void autoFlushOnMemTableThreshold() {
    try (Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("auto"), 40L, 2)) {
      for (int i = 0; i < 20; i++) {
        lsm.put(bytes("k" + i), bytes("value-" + i));
      }

      assertThat(lsm.sstableCount()).isGreaterThan(0);
      for (int i = 0; i < 20; i++) {
        assertThat(value(lsm.get(bytes("k" + i)))).isEqualTo("value-" + i);
      }
    }
  }

  @Test
  @DisplayName("Переоткрытие каталога загружает SSTable и перестраивает индекс")
  void reopenLoadsSstables() {
    Path directory = tempDir.resolve("reopen");
    try (Kata21_LsmMemTableAndSstable lsm = newLsm(directory, LARGE_MEMTABLE, 4)) {
      for (int i = 0; i < 20; i++) {
        lsm.put(bytes("key-" + i), bytes("value-" + i));
      }
      lsm.delete(bytes("key-7"));
    }

    try (Kata21_LsmMemTableAndSstable lsm = newLsm(directory, LARGE_MEMTABLE, 4)) {
      assertThat(lsm.sstableCount()).isEqualTo(1);
      for (int i = 0; i < 20; i++) {
        if (i == 7) {
          assertThat(lsm.get(bytes("key-" + i))).isEmpty();
        } else {
          assertThat(value(lsm.get(bytes("key-" + i)))).isEqualTo("value-" + i);
        }
      }
      lsm.put(bytes("after-reopen"), bytes("ok"));
    }

    try (Kata21_LsmMemTableAndSstable lsm = newLsm(directory, LARGE_MEMTABLE, 4)) {
      assertThat(value(lsm.get(bytes("after-reopen")))).isEqualTo("ok");
    }
  }

  @Test
  @DisplayName("Пустые ключи и значения допустимы")
  void emptyKeyAndValue() {
    try (Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("empty"), LARGE_MEMTABLE, 4)) {
      lsm.put(new byte[0], new byte[0]);
      assertThat(lsm.get(new byte[0])).isPresent();
      assertThat(lsm.get(new byte[0]).orElseThrow()).isEmpty();

      lsm.flush();
      assertThat(lsm.get(new byte[0])).isPresent();
      assertThat(lsm.get(new byte[0]).orElseThrow()).isEmpty();

      lsm.delete(new byte[0]);
      assertThat(lsm.get(new byte[0])).isEmpty();
    }
  }

  @Test
  @DisplayName("Беззнаковые бинарные ключи сохраняют порядок")
  void binaryKeysUseUnsignedOrder() {
    byte[] zero = {(byte) 0x00};
    byte[] sevenF = {(byte) 0x7F};
    byte[] eightZero = {(byte) 0x80};
    byte[] ff = {(byte) 0xFF};

    try (Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("binary"), LARGE_MEMTABLE, 2)) {
      lsm.put(ff, bytes("ff"));
      lsm.put(zero, bytes("00"));
      lsm.put(eightZero, bytes("80"));
      lsm.put(sevenF, bytes("7f"));
      lsm.flush();

      assertThat(value(lsm.get(zero))).isEqualTo("00");
      assertThat(value(lsm.get(sevenF))).isEqualTo("7f");
      assertThat(value(lsm.get(eightZero))).isEqualTo("80");
      assertThat(value(lsm.get(ff))).isEqualTo("ff");
    }
  }

  @Test
  @DisplayName("Ключи и значения копируются; get не отдаёт внутренний массив")
  void defensiveCopies() {
    try (Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("copies"), LARGE_MEMTABLE, 4)) {
      byte[] key = bytes("key");
      byte[] value = bytes("value");
      lsm.put(key, value);
      key[0] = 'X';
      value[0] = 'X';

      assertThat(value(lsm.get(bytes("key")))).isEqualTo("value");

      byte[] returned = lsm.get(bytes("key")).orElseThrow();
      returned[0] = 'Y';
      assertThat(value(lsm.get(bytes("key")))).isEqualTo("value");
    }
  }

  @Test
  @DisplayName("flush на пустой MemTable — no-op")
  void flushEmptyMemTable() {
    try (Kata21_LsmMemTableAndSstable lsm =
        newLsm(tempDir.resolve("flush-empty"), LARGE_MEMTABLE, 4)) {
      lsm.flush();
      assertThat(lsm.sstableCount()).isZero();
    }
  }

  @Test
  @DisplayName("Одна запись больше порога сразу уходит в SSTable")
  void oversizedEntryFlushesImmediately() {
    try (Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("oversized"), 1L, 4)) {
      lsm.put(bytes("key"), bytes("value"));
      assertThat(lsm.sstableCount()).isEqualTo(1);
      assertThat(value(lsm.get(bytes("key")))).isEqualTo("value");
    }
  }

  @Test
  @DisplayName("Валидация аргументов и null-значений")
  void validation() {
    assertThatThrownBy(() -> newLsm(null, 100L, 4)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newLsm(tempDir.resolve("invalid1"), 0L, 4))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newLsm(tempDir.resolve("invalid2"), 100L, 0))
        .isInstanceOf(IllegalArgumentException.class);

    try (Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("nulls"), LARGE_MEMTABLE, 4)) {
      assertThatThrownBy(() -> lsm.put(null, bytes("v"))).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> lsm.put(bytes("k"), null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> lsm.delete(null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> lsm.get(null)).isInstanceOf(NullPointerException.class);
    }
  }

  @Test
  @DisplayName("close идемпотентен, после close операции запрещены")
  void closeIsIdempotentAndBlocksOperations() {
    Kata21_LsmMemTableAndSstable lsm = newLsm(tempDir.resolve("close"), LARGE_MEMTABLE, 4);
    lsm.put(bytes("key"), bytes("value"));
    lsm.close();
    lsm.close();

    assertThatThrownBy(() -> lsm.get(bytes("key"))).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> lsm.put(bytes("a"), bytes("b")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> lsm.delete(bytes("a"))).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(lsm::flush).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(lsm::sstableCount).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Удаление отсутствующего ключа создаёт tombstone, put после delete разрешён")
  void deleteMissingThenPut() {
    try (Kata21_LsmMemTableAndSstable lsm =
        newLsm(tempDir.resolve("delete-missing"), LARGE_MEMTABLE, 4)) {
      lsm.delete(bytes("ghost"));
      assertThat(lsm.get(bytes("ghost"))).isEmpty();

      lsm.put(bytes("ghost"), bytes("back"));
      assertThat(value(lsm.get(bytes("ghost")))).isEqualTo("back");
    }
  }

  @Test
  @DisplayName("SSTable-файлы создаются в каталоге и переживают close")
  void sstableFilesExistAfterClose() throws Exception {
    Path directory = tempDir.resolve("files");
    try (Kata21_LsmMemTableAndSstable lsm = newLsm(directory, LARGE_MEMTABLE, 4)) {
      lsm.put(bytes("key"), bytes("value"));
    }

    try (var files = Files.list(directory)) {
      assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(1L);
    }
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static String value(Optional<byte[]> value) {
    return value.map(bytes -> new String(bytes, StandardCharsets.UTF_8)).orElse(null);
  }
}
