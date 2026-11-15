package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy21_SetSymmetricDifferenceSolution;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 21: Set symmetric difference and subset")
class Easy21_SetSymmetricDifferenceTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy21_SetSymmetricDifference newSolution() {
    return USE_SOLUTION
        ? new Easy21_SetSymmetricDifferenceSolution()
        : new Easy21_SetSymmetricDifference();
  }

  @Test
  @DisplayName("Симметрическая разность с пересечением")
  void symmetricDifferenceWithOverlap() {
    assertThat(newSolution().symmetricDifference(Set.of(1, 2, 3), Set.of(3, 4)))
        .containsExactlyInAnyOrder(1, 2, 4);
  }

  @Test
  @DisplayName("Непересекающиеся и равные множества")
  void disjointAndEqual() {
    assertThat(newSolution().symmetricDifference(Set.of(1, 2), Set.of(3, 4)))
        .containsExactlyInAnyOrder(1, 2, 3, 4);
    assertThat(newSolution().symmetricDifference(Set.of(1, 2, 3), Set.of(1, 2, 3))).isEmpty();
    assertThat(newSolution().symmetricDifference(Set.of(), Set.of())).isEmpty();
    assertThat(newSolution().symmetricDifference(Set.of(1), Set.of())).containsExactly(1);
  }

  @Test
  @DisplayName("Входные множества не изменяются")
  void inputsAreNotMutated() {
    Set<Integer> a = new HashSet<>(Set.of(1, 2, 3));
    Set<Integer> b = new HashSet<>(Set.of(3, 4));

    newSolution().symmetricDifference(a, b);

    assertThat(a).containsExactlyInAnyOrder(1, 2, 3);
    assertThat(b).containsExactlyInAnyOrder(3, 4);
  }

  @Test
  @DisplayName("isSubset: true, false, равенство и пустое множество")
  void subsetChecks() {
    assertThat(newSolution().isSubset(Set.of(1, 2), Set.of(1, 2, 3))).isTrue();
    assertThat(newSolution().isSubset(Set.of(1, 4), Set.of(1, 2, 3))).isFalse();
    assertThat(newSolution().isSubset(Set.of(1, 2), Set.of(1, 2))).isTrue();
    assertThat(newSolution().isSubset(Set.of(), Set.of(1))).isTrue();
    assertThat(newSolution().isSubset(Set.of(1), Set.of())).isFalse();
  }

  @Test
  @DisplayName("Результат симметрической разности неизменяем")
  void immutableResult() {
    Set<Integer> result = newSolution().symmetricDifference(Set.of(1), Set.of(2));
    assertThatThrownBy(() -> result.add(3)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("null-множества отклоняются")
  void nullArguments() {
    assertThatThrownBy(() -> newSolution().symmetricDifference(null, Set.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().symmetricDifference(Set.of(), null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().isSubset(null, Set.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().isSubset(Set.of(), null))
        .isInstanceOf(NullPointerException.class);
  }
}
