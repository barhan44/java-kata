package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy28_DeduplicatePreservingLatest.Message;
import dev.kata.solutions.easy.Easy28_DeduplicatePreservingLatestSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 28: Deduplicate preserving latest")
class Easy28_DeduplicatePreservingLatestTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy28_DeduplicatePreservingLatest newSolution() {
    return USE_SOLUTION
        ? new Easy28_DeduplicatePreservingLatestSolution()
        : new Easy28_DeduplicatePreservingLatest();
  }

  @Test
  @DisplayName("Оставляет последний payload и порядок последних вхождений")
  void keepsLatestPayloadAndLastOrder() {
    List<Message> messages =
        List.of(
            new Message("a", "old"),
            new Message("b", "x"),
            new Message("a", "new"),
            new Message("c", "y"),
            new Message("b", "z"));

    assertThat(newSolution().deduplicate(messages))
        .containsExactly(new Message("a", "new"), new Message("c", "y"), new Message("b", "z"));
  }

  @Test
  @DisplayName("Уникальные сообщения сохраняют исходный порядок")
  void uniqueMessagesPreserved() {
    List<Message> messages =
        List.of(new Message("a", "1"), new Message("b", "2"), new Message("c", "3"));
    assertThat(newSolution().deduplicate(messages)).containsExactlyElementsOf(messages);
  }

  @Test
  @DisplayName("Все сообщения с одним id — остаётся последнее")
  void allSameId() {
    assertThat(
            newSolution()
                .deduplicate(
                    List.of(new Message("a", "1"), new Message("a", "2"), new Message("a", "3"))))
        .containsExactly(new Message("a", "3"));
  }

  @Test
  @DisplayName("Пустой список")
  void emptyList() {
    assertThat(newSolution().deduplicate(List.of())).isEmpty();
  }

  @Test
  @DisplayName("Исходный список не меняется, результат неизменяем")
  void immutability() {
    List<Message> input = List.of(new Message("a", "1"), new Message("a", "2"));
    List<Message> result = newSolution().deduplicate(input);

    assertThat(input).containsExactly(new Message("a", "1"), new Message("a", "2"));
    assertThatThrownBy(() -> result.add(new Message("b", "3")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация Message и null-списка")
  void validation() {
    assertThatThrownBy(() -> new Message(null, "p")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Message(" ", "p")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Message("a", null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().deduplicate(null))
        .isInstanceOf(NullPointerException.class);
  }
}
