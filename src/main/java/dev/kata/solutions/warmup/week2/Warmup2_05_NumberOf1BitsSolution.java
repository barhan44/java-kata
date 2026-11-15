package dev.kata.solutions.warmup.week2;

import dev.kata.warmup.week2.Warmup2_05_NumberOf1Bits;

/**
 * Эталон Warmup 2.5.
 *
 * <p>{@code n & (n - 1)} обнуляет младший единичный бит: вычитание единицы инвертирует хвост нулей
 * и первый единичный бит, а AND оставляет только старшую часть. Количество итераций равно числу
 * единиц, а не 32. Для {@code n == 0} цикл не выполняется вовсе.
 *
 * <p>Знаковый {@code int} не мешает: побитовые операции работают с дополнительным кодом, и {@code
 * -1 & -2 == -2} корректно снимает один бит.
 */
public final class Warmup2_05_NumberOf1BitsSolution extends Warmup2_05_NumberOf1Bits {

  @Override
  public int hammingWeight(int n) {
    int count = 0;
    while (n != 0) {
      n &= n - 1;
      count++;
    }
    return count;
  }
}
