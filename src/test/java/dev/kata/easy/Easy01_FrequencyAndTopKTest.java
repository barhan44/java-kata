package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy01_FrequencyAndTopKSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 01: Frequency and Top-K words")
class Easy01_FrequencyAndTopKTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy01_FrequencyAndTopK newSolution() {
    return USE_SOLUTION ? new Easy01_FrequencyAndTopKSolution() : new Easy01_FrequencyAndTopK();
  }

  @Test
  @DisplayName("Классический пример: частота и порядок")
  void classicExample() {
    List<String> words =
        List.of("the", "day", "is", "sunny", "the", "the", "the", "sunny", "is", "is");
    assertThat(newSolution().topKFrequentWords(words, 4))
        .containsExactly("the", "is", "sunny", "day");
  }

  @Test
  @DisplayName("Регистр, пробелы, blank и null нормализуются")
  void normalization() {
    List<String> words =
        java.util.Arrays.asList("Apple", " apple ", "BANANA", "banana", "  ", null, "");
    assertThat(newSolution().topKFrequentWords(words, 2)).containsExactly("apple", "banana");
  }

  @Test
  @DisplayName("При равной частоте — алфавитный порядок")
  void tieBreakAlphabetical() {
    List<String> words = List.of("b", "a", "c", "a", "b", "d");
    assertThat(newSolution().topKFrequentWords(words, 3)).containsExactly("a", "b", "c");
  }

  @Test
  @DisplayName("k = 0 и k больше числа уникальных слов")
  void kBoundaries() {
    assertThat(newSolution().topKFrequentWords(List.of("a", "b"), 0)).isEmpty();
    assertThat(newSolution().topKFrequentWords(List.of("a", "b", "a"), 10))
        .containsExactly("a", "b");
    assertThat(newSolution().topKFrequentWords(List.of(), 3)).isEmpty();
  }

  @Test
  @DisplayName("Отрицательный k и null-список отклоняются")
  void invalidArguments() {
    assertThatThrownBy(() -> newSolution().topKFrequentWords(List.of("a"), -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().topKFrequentWords(null, 1))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<String> result = newSolution().topKFrequentWords(List.of("a"), 1);
    assertThatThrownBy(() -> result.add("b")).isInstanceOf(UnsupportedOperationException.class);
  }
}
