package dev.kata.easy;

import java.util.List;

/**
 * Easy 19. Транспонирование матрицы (IntStream).
 *
 * <h2>Задача</h2>
 *
 * <p>Транспонировать прямоугольную матрицу, представленную списком строк. Проверить, что все строки
 * имеют одинаковую длину; иначе бросить {@link IllegalArgumentException}. Пустая матрица
 * транспонируется в пустую.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * transpose([[1,2,3],[4,5,6]]) → [[1,4],[2,5],[3,6]]
 * transpose([[1,2],[3,4]])     → [[1,3],[2,4]]
 * transpose([])                → []
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ число строк и столбцов; все строки одинаковой длины;
 *   <li>элементы могут быть любого типа, в том числе {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(m × n), память O(m × n) на результат. Эталон перебирает индексы столбцов через {@code
 * IntStream.range}, а внутри — строки; ручной аккумулятор не нужен. Внешний и внутренние списки
 * неизменяемы.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy19_TransposeMatrixListSolution}.
 */
public class Easy19_TransposeMatrixList {

  /** Транспонирует прямоугольную матрицу. */
  public <T> List<List<T>> transpose(List<List<T>> matrix) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
