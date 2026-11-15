package dev.kata.warmup.week2;

/**
 * Warmup 2.6 — Reverse Bits (LeetCode #190).
 *
 * <h2>Условие</h2>
 *
 * <p>Развернуть порядок битов 32-битного беззнакового целого: младший бит входа становится старшим
 * битом выхода и наоборот. Знак результата интерпретируется как обычный Java {@code int}.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * reverseBits(43261596)  → 964176192
 * reverseBits(1)         → -2147483648  // 0x80000000
 * reverseBits(0)         → 0
 * reverseBits(-1)        → -1
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <p>Вход — любое 32-битное значение. Ровно 32 итерации, без зависимостей от знака.
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(32) = O(1), память O(1). На каждой итерации берём младший бит через {@code n & 1},
 * сдвигаем результат влево и добавляем бит, затем беззнаково сдвигаем {@code n >>>= 1}. Обычный
 * {@code >>} для отрицательных чисел тянул бы единицу из знака бесконечно.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week2.Warmup2_06_ReverseBitsSolution}.
 */
public class Warmup2_06_ReverseBits {

  /** Разворачивает 32 бита значения. */
  public int reverseBits(int n) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
