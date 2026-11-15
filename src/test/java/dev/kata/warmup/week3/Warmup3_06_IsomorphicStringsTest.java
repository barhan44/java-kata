package dev.kata.warmup.week3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week3.Warmup3_06_IsomorphicStringsSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 3.6: Isomorphic Strings")
class Warmup3_06_IsomorphicStringsTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup3_06_IsomorphicStrings newSolution() {
    return USE_SOLUTION
        ? new Warmup3_06_IsomorphicStringsSolution()
        : new Warmup3_06_IsomorphicStrings();
  }

  @Test
  @DisplayName("Классические примеры")
  void examples() {
    assertThat(newSolution().isIsomorphic("egg", "add")).isTrue();
    assertThat(newSolution().isIsomorphic("foo", "bar")).isFalse();
    assertThat(newSolution().isIsomorphic("paper", "title")).isTrue();
    assertThat(newSolution().isIsomorphic("badc", "baba")).isFalse();
  }

  @Test
  @DisplayName("Пустые и одинаковые строки")
  void emptyAndSame() {
    assertThat(newSolution().isIsomorphic("", "")).isTrue();
    assertThat(newSolution().isIsomorphic("abc", "abc")).isTrue();
    assertThat(newSolution().isIsomorphic("a", "b")).isTrue();
  }

  @Test
  @DisplayName("Многие в один и один во многие запрещены")
  void bidirectionalMapping() {
    assertThat(newSolution().isIsomorphic("ab", "aa")).isFalse();
    assertThat(newSolution().isIsomorphic("aa", "ab")).isFalse();
    assertThat(newSolution().isIsomorphic("abc", "xyz")).isTrue();
  }

  @Test
  @DisplayName("Разные длины и null отклоняются")
  void invalidInput() {
    assertThat(newSolution().isIsomorphic("a", "ab")).isFalse();
    assertThatThrownBy(() -> newSolution().isIsomorphic(null, "a"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().isIsomorphic("a", null))
        .isInstanceOf(NullPointerException.class);
  }
}
