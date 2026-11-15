package dev.kata.solutions.algorithms;

import dev.kata.algorithms.Kata13_BloomFilter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Эталонное решение Kata 13.
 *
 * <h2>Расчёт m и k</h2>
 *
 * <p>Используются классические формулы оптимального фильтра Блума:
 *
 * <pre>
 *   m = ceil(-n * ln(p) / (ln 2)^2)
 *   k = round(m / n * ln 2)
 * </pre>
 *
 * <p>{@code m} — минимальный размер битового массива, при котором при {@code k} хешах вероятность
 * ложного срабатывания не превышает {@code p}. Округление вверх для {@code m} и обычное округление
 * для {@code k} могут дать фактическую вероятность чуть ниже целевой — это безопасное направление.
 * {@code Math.max(1, ...)} защищает от вырожденных случаев вроде {@code p -> 1}.
 *
 * <h2>Хеширование: два базовых хеша вместо k</h2>
 *
 * <p>Реализована FNV-1a 64-бит с финализатором MurmurHash3. FNV-1a линейна по XOR и умножению, но
 * финализатор {@code fmix64} разбивает корреляции и даёт лавинное перемешивание. Два независимых
 * seed-а дают {@code h1} и {@code h2}; {@code h2} принудительно делается нечётным, чтобы при чётном
 * {@code m} последовательность {@code h1 + i*h2} не «зацикливалась» на подгруппе.
 *
 * <p>Техника Kirsch–Mitzenmacher: {@code g_i = h1 + i*h2 mod m}. Асимптотически это эквивалентно
 * {@code k} независимым хешам, но требует двух проходов по элементу вместо {@code k}. На практике
 * для фильтра Блума этого достаточно; эмпирический тест на 50 000 элементов проверяет, что
 * фактическая доля ложных срабатываний не превышает целевую с учётом статистической погрешности.
 *
 * <h2>Битовая упаковка</h2>
 *
 * <p>Биты лежат в {@code long[]}. Для индекса {@code bit}:
 *
 * <pre>
 *   word  = (int) (bit >>> 6)
 *   mask  = 1L << (bit & 63)
 *   bits[word] |= mask
 * </pre>
 *
 * <p>Сдвиг на {@code bit & 63} обязателен: Java маскирует расстояние сдвига до 6 младших бит, но
 * явная маска делает намерение очевидным и не зависит от неявного поведения. Один {@code long}
 * покрывает 64 бита, поэтому обращений к памяти в 8 раз меньше, чем у {@code boolean[]}, и они
 * попадают в одну cache line для соседних битов.
 *
 * <h2>Аллокации</h2>
 *
 * <p>{@code put(byte[])} и {@code mightContain(byte[])} не аллоцируют: хеши считаются в цикле по
 * массиву, биты устанавливаются на месте. {@code put(String)}/{@code mightContain(String)}
 * вынуждены вызвать {@code getBytes(UTF_8)} — это временный массив. Для горячего пути используйте
 * байтовые перегрузки.
 *
 * <h2>Потокобезопасность</h2>
 *
 * <p>Класс намеренно не синхронизирован: {@code put} — это несколько независимых OR-ов, а {@code
 * mightContain} может прочитать слово в промежуточном состоянии. Для конкурентного использования
 * нужна внешняя синхронизация; для сценария «построили один раз — читаем параллельно» достаточно
 * publish-safe публикации (например, {@code final}/{@code volatile} ссылки), потому что после
 * завершения всех {@code put} массив больше не меняется.
 */
public final class Kata13_BloomFilterSolution extends Kata13_BloomFilter {

  private static final double LN2 = Math.log(2.0);

  /** Максимальное число бит, адресуемое int-индексом слова. */
  private static final long MAX_BITS = (long) Integer.MAX_VALUE * 64L;

  private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
  private static final long FNV_PRIME = 0x100000001b3L;

  /** Два независимых seed-а для h1/h2. */
  private static final long SEED_1 = 0x243F6A8885A308D3L;

  private static final long SEED_2 = 0x13198A2E03707344L;

  private final long bitSize;
  private final int hashFunctions;
  private final long[] bits;

  private Kata13_BloomFilterSolution(
      long expectedInsertions, double falsePositiveProbability, long bitSize, int hashFunctions) {
    super(expectedInsertions, falsePositiveProbability);
    this.bitSize = bitSize;
    this.hashFunctions = hashFunctions;
    this.bits = new long[(int) ((bitSize + 63L) >>> 6)];
  }

  /**
   * Фабрика с валидацией и расчётом параметров.
   *
   * <p>Статический метод скрывает (hides) метод заготовки: тест вызывает {@code
   * Kata13_BloomFilterSolution.create(...)} в режиме {@code -DuseSolution=true}.
   */
  public static Kata13_BloomFilterSolution create(
      long expectedInsertions, double falsePositiveProbability) {
    if (expectedInsertions <= 0) {
      throw new IllegalArgumentException(
          "expectedInsertions must be > 0, got " + expectedInsertions);
    }
    if (!(falsePositiveProbability > 0.0 && falsePositiveProbability < 1.0)) {
      throw new IllegalArgumentException(
          "falsePositiveProbability must be in (0, 1), got " + falsePositiveProbability);
    }

    double computedBits = -expectedInsertions * Math.log(falsePositiveProbability) / (LN2 * LN2);
    if (!(computedBits > 0.0) || computedBits > MAX_BITS) {
      throw new IllegalArgumentException(
          "computed bit array is not addressable: " + computedBits + " bits");
    }

    long bitSize = Math.max(1L, (long) Math.ceil(computedBits));
    int hashFunctions = (int) Math.max(1L, Math.round((double) bitSize / expectedInsertions * LN2));
    return new Kata13_BloomFilterSolution(
        expectedInsertions, falsePositiveProbability, bitSize, hashFunctions);
  }

  @Override
  public void put(byte[] item) {
    Objects.requireNonNull(item, "item");
    long h1 = hash64(item, SEED_1);
    long h2 = hash64(item, SEED_2) | 1L; // нечётный шаг: нет вырожденной подгруппы при чётном m
    for (int i = 0; i < hashFunctions; i++) {
      setBit(Math.floorMod(h1 + i * h2, bitSize));
    }
  }

  @Override
  public void put(String item) {
    Objects.requireNonNull(item, "item");
    put(item.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public boolean mightContain(byte[] item) {
    Objects.requireNonNull(item, "item");
    long h1 = hash64(item, SEED_1);
    long h2 = hash64(item, SEED_2) | 1L;
    for (int i = 0; i < hashFunctions; i++) {
      if (!getBit(Math.floorMod(h1 + i * h2, bitSize))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean mightContain(String item) {
    Objects.requireNonNull(item, "item");
    return mightContain(item.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public double expectedFalsePositiveProbability() {
    return falsePositiveProbability;
  }

  @Override
  public long bitSize() {
    return bitSize;
  }

  @Override
  public int hashFunctions() {
    return hashFunctions;
  }

  private void setBit(long bitIndex) {
    int word = (int) (bitIndex >>> 6);
    int offset = (int) (bitIndex & 63L);
    bits[word] |= 1L << offset;
  }

  private boolean getBit(long bitIndex) {
    int word = (int) (bitIndex >>> 6);
    int offset = (int) (bitIndex & 63L);
    return (bits[word] & (1L << offset)) != 0L;
  }

  /** FNV-1a 64 с seed-ом и финализатором MurmurHash3. */
  private static long hash64(byte[] item, long seed) {
    long hash = FNV_OFFSET_BASIS ^ seed;
    for (byte b : item) {
      hash ^= b & 0xFF;
      hash *= FNV_PRIME;
    }
    return fmix64(hash);
  }

  /** Финальный миксер MurmurHash3: лавинное перемешивание всех 64 бит. */
  private static long fmix64(long value) {
    value ^= value >>> 33;
    value *= 0xff51afd7ed558ccdL;
    value ^= value >>> 33;
    value *= 0xc4ceb9fe1a85ec53L;
    value ^= value >>> 33;
    return value;
  }
}
