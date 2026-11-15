package dev.kata.solutions.easy;

import dev.kata.easy.Easy19_TransposeMatrixList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Эталон Easy 19.
 *
 * <p>Сначала проверяется прямоугольность: длина первой строки — эталон для остальных. Затем для
 * каждого столбца {@code IntStream.range} собирает значения по строкам. {@code
 * mapToObj(...).toList()} создаёт неизменяемую строку результата; внешний {@code toList()} —
 * неизменяемую матрицу.
 *
 * <p>Для матрицы с нулём строк или нулём столбцов результат пуст: транспонировать нечего. Элементы
 * не копируются, поэтому {@code null}-значения допустимы.
 */
public final class Easy19_TransposeMatrixListSolution extends Easy19_TransposeMatrixList {

  @Override
  public <T> List<List<T>> transpose(List<List<T>> matrix) {
    Objects.requireNonNull(matrix, "matrix");
    if (matrix.isEmpty()) {
      return List.of();
    }

    int columns = Objects.requireNonNull(matrix.get(0), "row").size();
    for (List<T> row : matrix) {
      Objects.requireNonNull(row, "row");
      if (row.size() != columns) {
        throw new IllegalArgumentException(
            "Matrix must be rectangular: expected " + columns + " columns, got " + row.size());
      }
    }
    if (columns == 0) {
      return List.of();
    }

    int rows = matrix.size();
    return IntStream.range(0, columns)
        .mapToObj(
            column ->
                IntStream.range(0, rows).mapToObj(row -> matrix.get(row).get(column)).toList())
        .toList();
  }
}
