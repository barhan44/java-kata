package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy19_TransposeMatrixListSolution;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 19: Transpose matrix list")
class Easy19_TransposeMatrixListTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy19_TransposeMatrixList newSolution() {
    return USE_SOLUTION
        ? new Easy19_TransposeMatrixListSolution()
        : new Easy19_TransposeMatrixList();
  }

  @Test
  @DisplayName("Прямоугольная матрица 2×3")
  void transposesRectangularMatrix() {
    List<List<Integer>> matrix = List.of(List.of(1, 2, 3), List.of(4, 5, 6));

    assertThat(newSolution().transpose(matrix))
        .containsExactly(List.of(1, 4), List.of(2, 5), List.of(3, 6));
  }

  @Test
  @DisplayName("Квадратная матрица и строки/столбцы")
  void squareAndSingleDimension() {
    assertThat(newSolution().transpose(List.of(List.of(1, 2), List.of(3, 4))))
        .containsExactly(List.of(1, 3), List.of(2, 4));
    assertThat(newSolution().transpose(List.of(List.of(1, 2, 3))))
        .containsExactly(List.of(1), List.of(2), List.of(3));
    assertThat(newSolution().transpose(List.of(List.of(1), List.of(2))))
        .containsExactly(List.of(1, 2));
  }

  @Test
  @DisplayName("Пустая матрица и матрица из пустых строк")
  void emptyMatrix() {
    assertThat(newSolution().transpose(List.of())).isEmpty();
    assertThat(newSolution().transpose(List.of(List.of(), List.of()))).isEmpty();
  }

  @Test
  @DisplayName("Непрямоугольная матрица отклоняется")
  void jaggedMatrix() {
    assertThatThrownBy(() -> newSolution().transpose(List.of(List.of(1, 2), List.of(3))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("null-матрица и null-строка отклоняются")
  void nullArguments() {
    assertThatThrownBy(() -> newSolution().transpose(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().transpose(Arrays.asList(List.of(1), null)))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<List<String>> result = newSolution().transpose(List.of(List.of("a", "b")));

    assertThat(result).containsExactly(List.of("a"), List.of("b"));
    assertThatThrownBy(() -> result.add(List.of("c")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.get(0).add("c"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
