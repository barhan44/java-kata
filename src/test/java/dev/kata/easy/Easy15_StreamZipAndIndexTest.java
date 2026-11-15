package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy15_StreamZipAndIndexSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 15: Zip with index")
class Easy15_StreamZipAndIndexTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy15_StreamZipAndIndex newSolution() {
    return USE_SOLUTION ? new Easy15_StreamZipAndIndexSolution() : new Easy15_StreamZipAndIndex();
  }

  @Test
  @DisplayName("Объединяет списки с индексом")
  void zipsWithIndex() {
    List<String> result =
        newSolution()
            .zipWithIndex(
                List.of("a", "b"),
                List.of(10, 20),
                (index, word, number) -> index + ":" + word + number);

    assertThat(result).containsExactly("0:a10", "1:b20");
  }

  @Test
  @DisplayName("Длина результата — минимум из двух списков")
  void usesShorterLength() {
    assertThat(
            newSolution()
                .zipWithIndex(
                    List.of(1, 2, 3),
                    List.of("a", "b"),
                    (index, number, letter) -> number + letter))
        .containsExactly("1a", "2b");
    assertThat(
            newSolution()
                .zipWithIndex(
                    List.of(1), List.of("a", "b", "c"), (index, number, letter) -> number + letter))
        .containsExactly("1a");
  }

  @Test
  @DisplayName("Пустые списки дают пустой результат")
  void emptyLists() {
    assertThat(newSolution().zipWithIndex(List.of(), List.of(1), (index, a, b) -> a + ":" + b))
        .isEmpty();
    assertThat(newSolution().zipWithIndex(List.of(1), List.of(), (index, a, b) -> a + ":" + b))
        .isEmpty();
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<String> result =
        newSolution().zipWithIndex(List.of(1), List.of(2), (i, a, b) -> "" + (a + b));
    assertThatThrownBy(() -> result.add("x")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("null-аргументы отклоняются")
  void validation() {
    assertThatThrownBy(() -> newSolution().zipWithIndex(null, List.of(), (i, a, b) -> ""))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().zipWithIndex(List.of(), null, (i, a, b) -> ""))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().zipWithIndex(List.of(), List.of(), null))
        .isInstanceOf(NullPointerException.class);
  }
}
