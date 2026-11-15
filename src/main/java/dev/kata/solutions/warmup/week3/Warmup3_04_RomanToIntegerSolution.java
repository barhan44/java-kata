package dev.kata.solutions.warmup.week3;

import dev.kata.warmup.week3.Warmup3_04_RomanToInteger;
import java.util.Objects;

/**
 * Эталон Warmup 3.4.
 *
 * <p>Обратный проход: сравниваем значение символа с уже обработанным правым соседом. Если текущий
 * меньше правого, он является «вычитаемой» частью пары (IV, IX, XL, ...), поэтому вычитается; иначе
 * прибавляется. Так одна проверка покрывает все шесть вычитающих комбинаций.
 *
 * <p>{@code switch} expression возвращает значение и делает невозможный символ исключением: {@code
 * default -> throw}. Никаких map с боксом не требуется.
 */
public final class Warmup3_04_RomanToIntegerSolution extends Warmup3_04_RomanToInteger {

  @Override
  public int romanToInt(String s) {
    Objects.requireNonNull(s, "s");

    int total = 0;
    int previous = 0;
    for (int i = s.length() - 1; i >= 0; i--) {
      int value = valueOf(s.charAt(i));
      if (value < previous) {
        total -= value;
      } else {
        total += value;
        previous = value;
      }
    }
    return total;
  }

  private static int valueOf(char symbol) {
    return switch (symbol) {
      case 'I' -> 1;
      case 'V' -> 5;
      case 'X' -> 10;
      case 'L' -> 50;
      case 'C' -> 100;
      case 'D' -> 500;
      case 'M' -> 1000;
      default -> throw new IllegalArgumentException("Invalid Roman symbol: " + symbol);
    };
  }
}
