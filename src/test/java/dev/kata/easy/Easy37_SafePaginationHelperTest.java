package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy37_SafePaginationHelper.Page;
import dev.kata.solutions.easy.Easy37_SafePaginationHelperSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 37: Safe pagination")
class Easy37_SafePaginationHelperTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy37_SafePaginationHelper newSolution() {
    return USE_SOLUTION
        ? new Easy37_SafePaginationHelperSolution()
        : new Easy37_SafePaginationHelper();
  }

  @Test
  @DisplayName("Средняя и последняя страница")
  void middleAndLastPage() {
    List<Integer> all = List.of(1, 2, 3, 4, 5);

    Page<Integer> middle = newSolution().paginate(all, 2, 2);
    assertThat(middle.items()).containsExactly(3, 4);
    assertThat(middle.pageNumber()).isEqualTo(2);
    assertThat(middle.pageSize()).isEqualTo(2);
    assertThat(middle.totalItems()).isEqualTo(5);
    assertThat(middle.totalPages()).isEqualTo(3);

    Page<Integer> last = newSolution().paginate(all, 3, 2);
    assertThat(last.items()).containsExactly(5);
  }

  @Test
  @DisplayName("Страница за пределами пустая, метаданные сохраняются")
  void beyondLastPage() {
    Page<Integer> page = newSolution().paginate(List.of(1, 2, 3, 4, 5), 4, 2);

    assertThat(page.items()).isEmpty();
    assertThat(page.pageNumber()).isEqualTo(4);
    assertThat(page.totalItems()).isEqualTo(5);
    assertThat(page.totalPages()).isEqualTo(3);
  }

  @Test
  @DisplayName("Точное деление и размер больше списка")
  void exactDivisionAndOversizedPage() {
    Page<Integer> exact = newSolution().paginate(List.of(1, 2, 3, 4), 2, 2);
    assertThat(exact.items()).containsExactly(3, 4);
    assertThat(exact.totalPages()).isEqualTo(2);

    Page<Integer> oversized = newSolution().paginate(List.of(1, 2, 3), 1, 10);
    assertThat(oversized.items()).containsExactly(1, 2, 3);
    assertThat(oversized.totalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("Пустой список и огромный номер страницы")
  void emptyAndHugePageNumber() {
    Page<Integer> empty = newSolution().paginate(List.of(), 1, 5);
    assertThat(empty.items()).isEmpty();
    assertThat(empty.totalItems()).isZero();
    assertThat(empty.totalPages()).isZero();

    Page<Integer> huge =
        newSolution().paginate(List.of(1, 2), Integer.MAX_VALUE, Integer.MAX_VALUE);
    assertThat(huge.items()).isEmpty();
    assertThat(huge.totalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("Результат неизменяем, вход не меняется")
  void immutability() {
    List<String> all = List.of("a", "b");
    Page<String> page = newSolution().paginate(all, 1, 1);

    assertThat(all).containsExactly("a", "b");
    assertThatThrownBy(() -> page.items().add("c"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация аргументов")
  void validation() {
    assertThatThrownBy(() -> newSolution().paginate(List.of(), 0, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().paginate(List.of(), 1, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().paginate(List.of(), 1, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().paginate(null, 1, 1))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Page<>(List.of(), 0, 1, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
