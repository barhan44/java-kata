package dev.kata.warmup.week3;

import java.util.List;

/**
 * Warmup 3.1 — FizzBuzz (LeetCode #412).
 *
 * <h2>Условие</h2>
 *
 * <p>Для чисел от 1 до {@code n} вернуть строки по правилам: кратные 3 — {@code "Fizz"}, кратные 5
 * — {@code "Buzz"}, кратные и 3, и 5 — {@code "FizzBuzz"}, остальные — десятичное представление
 * числа. Реализовать в функциональном стиле через {@code IntStream.rangeClosed()}.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * fizzBuzz(15) → ["1","2","Fizz","4","Buzz","Fizz","7","8","Fizz","Buzz",
 *                 "11","Fizz","13","14","FizzBuzz"]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code n} ≤ 10⁴.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(n) на результат. Проверка кратности 15 первой (или {@code switch} по
 * {@code value % 15}) избавляет от вложенных условий; строки собираются лениво и не конкатенируются
 * в цикле.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week3.Warmup3_01_FizzBuzzSolution}.
 */
public class Warmup3_01_FizzBuzz {

  /** Возвращает FizzBuzz-последовательность от 1 до {@code n}. */
  public List<String> fizzBuzz(int n) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
