package dev.kata.solutions.warmup.week2;

import dev.kata.warmup.week2.Warmup2_06_ReverseBits;

/**
 * Эталон Warmup 2.6.
 *
 * <p>Ровно 32 итерации. {@code result <<= 1} освобождает младший бит, {@code n & 1} читает
 * очередной бит входа, {@code n >>>= 1} сдвигает вход беззнаково. Без {@code >>>} для
 * отрицательного входа знаковая единица подставлялась бы бесконечно, и результат был бы неверным.
 *
 * <p>Постоянное число итераций (а не {@code while (n != 0)}) важно: ведущие нули входа должны стать
 * хвостовыми нулями результата.
 */
public final class Warmup2_06_ReverseBitsSolution extends Warmup2_06_ReverseBits {

  @Override
  public int reverseBits(int n) {
    int result = 0;
    for (int i = 0; i < 32; i++) {
      result = (result << 1) | (n & 1);
      n >>>= 1;
    }
    return result;
  }
}
