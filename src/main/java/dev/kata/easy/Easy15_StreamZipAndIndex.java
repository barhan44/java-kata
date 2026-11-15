package dev.kata.easy;

import java.util.List;

/**
 * Easy 15. Zip двух списков с индексом.
 *
 * <h2>Задача</h2>
 *
 * <p>Объединить элементы двух списков по индексу: для каждой позиции {@code i} вызвать {@code
 * combiner.apply(i, first.get(i), second.get(i))}. Длина результата равна длине более короткого
 * списка. Индекс начинается с 0.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * zipWithIndex(["a","b"], [10,20], (i, s, n) -> i + ":" + s + n)
 *   → ["0:a10", "1:b20"]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ размеры списков;
 *   <li>{@code first}, {@code second} и {@code combiner} не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(min(n, m)), память O(min(n, m)) на результат. Эталон — {@code IntStream.range} по
 * короткой длине; списки не изменяются. Результат неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy15_StreamZipAndIndexSolution}.
 */
public class Easy15_StreamZipAndIndex {

  /** Функция от индекса и двух значений. */
  @FunctionalInterface
  public interface TriFunction<A, B, C, R> {
    R apply(A first, B second, C third);
  }

  /** Объединяет два списка по индексу. */
  public <A, B, R> List<R> zipWithIndex(
      List<A> first, List<B> second, TriFunction<Integer, A, B, R> combiner) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
