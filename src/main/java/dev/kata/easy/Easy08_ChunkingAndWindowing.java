package dev.kata.easy;

import java.util.List;

/**
 * Easy 08. Chunking и zipWithNext (IntStream + subList).
 *
 * <h2>Задача</h2>
 *
 * <ul>
 *   <li>{@code chunk(source, chunkSize)} — разбить список на подсписки длиной не более {@code
 *       chunkSize}; последний может быть короче;
 *   <li>{@code zipWithNext(source)} — список соседних пар: {@code [1,2,3,4] → [(1,2),(2,3),(3,4)]}.
 * </ul>
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * chunk([1,2,3,4,5], 2) → [[1,2],[3,4],[5]]
 * chunk([], 3)          → []
 * zipWithNext([1,2,3,4]) → [(1,2),(2,3),(3,4)]
 * zipWithNext([1])       → []
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code source.size()};
 *   <li>{@code chunkSize > 0}; элементы не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(n) на результат. Эталон использует {@code IntStream.range} с шагом {@code
 * chunkSize} и {@link List#subList} с последующим копированием. Внешний и внутренние списки
 * неизменяемы.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy08_ChunkingAndWindowingSolution}.
 */
public class Easy08_ChunkingAndWindowing {

  /** Пара соседних элементов. */
  public record Pair<A, B>(A first, B second) {}

  /** Разбивает список на куски размером до {@code chunkSize}. */
  public <T> List<List<T>> chunk(List<T> source, int chunkSize) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /** Возвращает пары соседних элементов. */
  public <T> List<Pair<T, T>> zipWithNext(List<T> source) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
