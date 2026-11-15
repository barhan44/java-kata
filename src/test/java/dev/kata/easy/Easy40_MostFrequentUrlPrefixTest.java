package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy40_MostFrequentUrlPrefixSolution;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 40: Most frequent URL prefixes")
class Easy40_MostFrequentUrlPrefixTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy40_MostFrequentUrlPrefix newSolution() {
    return USE_SOLUTION
        ? new Easy40_MostFrequentUrlPrefixSolution()
        : new Easy40_MostFrequentUrlPrefix();
  }

  @Test
  @DisplayName("Считает префиксы глубины 2, при равенстве — алфавит")
  void topPrefixes() {
    List<String> paths = List.of("/api/v1/users", "/api/v1/orders", "/api/v2/users", "/web/home");

    assertThat(newSolution().topPrefixSegments(paths, 2, 2)).containsExactly("/api/v1", "/api/v2");
    assertThat(newSolution().topPrefixSegments(paths, 1, 2)).containsExactly("/api", "/web");
  }

  @Test
  @DisplayName("Глубина 0 даёт корневой префикс")
  void zeroDepth() {
    assertThat(newSolution().topPrefixSegments(List.of("/api/v1", "/web", "/"), 0, 10))
        .containsExactly("/");
  }

  @Test
  @DisplayName("Query, fragment, двойные и хвостовые слеши игнорируются")
  void normalizesPaths() {
    List<String> paths = List.of("/api/v1/users?page=1", "/api/v1/users#top", "//api//v1//users//");

    assertThat(newSolution().topPrefixSegments(paths, 2, 10)).containsExactly("/api/v1");
  }

  @Test
  @DisplayName("Глубина больше длины пути сохраняет весь путь")
  void depthGreaterThanSegments() {
    assertThat(newSolution().topPrefixSegments(List.of("/a/b"), 10, 10)).containsExactly("/a/b");
  }

  @Test
  @DisplayName("Пустой вход и topK = 0")
  void emptyInputs() {
    assertThat(newSolution().topPrefixSegments(List.of(), 2, 5)).isEmpty();
    assertThat(newSolution().topPrefixSegments(List.of("/a"), 2, 0)).isEmpty();
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<String> result = newSolution().topPrefixSegments(List.of("/a"), 1, 1);
    assertThatThrownBy(() -> result.add("/b")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация depth, topK, null-списка и null-пути")
  void validation() {
    assertThatThrownBy(() -> newSolution().topPrefixSegments(List.of(), -1, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().topPrefixSegments(List.of(), 1, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().topPrefixSegments(null, 1, 1))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().topPrefixSegments(Arrays.asList("/a", null), 1, 1))
        .isInstanceOf(NullPointerException.class);
  }
}
