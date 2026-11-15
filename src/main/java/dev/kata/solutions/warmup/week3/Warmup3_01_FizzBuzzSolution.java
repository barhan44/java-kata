package dev.kata.solutions.warmup.week3;

import dev.kata.warmup.week3.Warmup3_01_FizzBuzz;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Эталон Warmup 3.1.
 *
 * <p>{@code IntStream.rangeClosed(1, n)} создаёт примитивный поток без боксинга на входе; {@code
 * mapToObj} превращает число в строку. Правило выбирается {@code switch} expression по остатку от
 * деления на 15: это компактнее цепочки {@code if}, а ветки покрывают все варианты. {@code
 * toList()} возвращает неизменяемый список.
 */
public final class Warmup3_01_FizzBuzzSolution extends Warmup3_01_FizzBuzz {

  @Override
  public List<String> fizzBuzz(int n) {
    if (n < 1) {
      throw new IllegalArgumentException("n must be >= 1, got " + n);
    }
    return IntStream.rangeClosed(1, n)
        .mapToObj(Warmup3_01_FizzBuzzSolution::fizzBuzzValue)
        .toList();
  }

  private static String fizzBuzzValue(int value) {
    return switch (value % 15) {
      case 0 -> "FizzBuzz";
      case 3, 6, 9, 12 -> "Fizz";
      case 5, 10 -> "Buzz";
      default -> Integer.toString(value);
    };
  }
}
