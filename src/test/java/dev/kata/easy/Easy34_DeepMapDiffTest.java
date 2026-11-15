package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy34_DeepMapDiffSolution;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 34: Deep map diff")
class Easy34_DeepMapDiffTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy34_DeepMapDiff newSolution() {
    return USE_SOLUTION ? new Easy34_DeepMapDiffSolution() : new Easy34_DeepMapDiff();
  }

  @Test
  @DisplayName("Различие вложенного листа и отсутствующий ключ")
  void nestedDifferences() {
    Map<String, Object> left =
        Map.of("server", Map.of("port", 8080, "host", "localhost"), "feature", Map.of());
    Map<String, Object> right =
        Map.of(
            "server", Map.of("port", 9090, "host", "localhost"),
            "feature", Map.of("flag", true));

    Map<String, String> result = newSolution().diffNestedMaps(left, right);
    assertThat(result.keySet()).containsExactly("feature.flag", "server.port");
    assertThat(result)
        .containsEntry("feature.flag", "feature.flag: <missing> -> true")
        .containsEntry("server.port", "server.port: 8080 -> 9090");
  }

  @Test
  @DisplayName("Одинаковые вложенные мапы не дают различий")
  void equalMaps() {
    Map<String, Object> left = Map.of("a", Map.of("b", 1), "c", "x");
    Map<String, Object> right = Map.of("a", Map.of("b", 1), "c", "x");

    assertThat(newSolution().diffNestedMaps(left, right)).isEmpty();
  }

  @Test
  @DisplayName("Лист с одной стороны и вложенная мапа с другой")
  void leafVersusNested() {
    Map<String, Object> left = Map.of("a", 1);
    Map<String, Object> right = Map.of("a", Map.of("b", 2));

    Map<String, String> result = newSolution().diffNestedMaps(left, right);
    assertThat(result.keySet()).containsExactly("a", "a.b");
    assertThat(result)
        .containsEntry("a", "a: 1 -> <missing>")
        .containsEntry("a.b", "a.b: <missing> -> 2");
  }

  @Test
  @DisplayName("null-лист отличается от отсутствия")
  void nullLeaf() {
    Map<String, Object> left = new HashMap<>();
    left.put("a", null);
    Map<String, Object> right = Map.of("b", 1);

    assertThat(newSolution().diffNestedMaps(left, right))
        .containsEntry("a", "a: null -> <missing>")
        .containsEntry("b", "b: <missing> -> 1");
  }

  @Test
  @DisplayName("Пустые мапы и неизменяемый результат")
  void emptyAndImmutable() {
    assertThat(newSolution().diffNestedMaps(Map.of(), Map.of())).isEmpty();

    Map<String, String> diff = newSolution().diffNestedMaps(Map.of("a", 1), Map.of("a", 2));
    assertThatThrownBy(() -> diff.put("b", "x")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Входные мапы не изменяются")
  void inputsUntouched() {
    Map<String, Object> left = new LinkedHashMap<>();
    left.put("a", 1);
    Map<String, Object> right = new LinkedHashMap<>();
    right.put("a", 2);

    newSolution().diffNestedMaps(left, right);

    assertThat(left).containsExactly(Map.entry("a", 1));
    assertThat(right).containsExactly(Map.entry("a", 2));
  }

  @Test
  @DisplayName("null-аргументы отклоняются")
  void nullArguments() {
    assertThatThrownBy(() -> newSolution().diffNestedMaps(null, Map.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().diffNestedMaps(Map.of(), null))
        .isInstanceOf(NullPointerException.class);
  }
}
