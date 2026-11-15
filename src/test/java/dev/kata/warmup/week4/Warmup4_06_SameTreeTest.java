package dev.kata.warmup.week4;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week4.Warmup4_06_SameTreeSolution;
import dev.kata.warmup.common.TreeNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 4.6: Same Tree")
class Warmup4_06_SameTreeTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup4_06_SameTree newSolution() {
    return USE_SOLUTION ? new Warmup4_06_SameTreeSolution() : new Warmup4_06_SameTree();
  }

  @Test
  @DisplayName("Идентичные деревья")
  void identical() {
    assertThat(newSolution().isSameTree(TreeNode.of(1, 2, 3), TreeNode.of(1, 2, 3))).isTrue();
    assertThat(
            newSolution()
                .isSameTree(TreeNode.of(4, 2, 7, 1, 3, 6, 9), TreeNode.of(4, 2, 7, 1, 3, 6, 9)))
        .isTrue();
  }

  @Test
  @DisplayName("Оба дерева пусты")
  void bothEmpty() {
    assertThat(newSolution().isSameTree(null, null)).isTrue();
  }

  @Test
  @DisplayName("Одно дерево пусто")
  void oneEmpty() {
    assertThat(newSolution().isSameTree(TreeNode.of(1), null)).isFalse();
    assertThat(newSolution().isSameTree(null, TreeNode.of(1))).isFalse();
  }

  @Test
  @DisplayName("Разные значения")
  void differentValues() {
    assertThat(newSolution().isSameTree(TreeNode.of(1, 2), TreeNode.of(1, 3))).isFalse();
    assertThat(newSolution().isSameTree(TreeNode.of(1), TreeNode.of(2))).isFalse();
  }

  @Test
  @DisplayName("Разная структура при одинаковых значениях")
  void differentStructure() {
    assertThat(newSolution().isSameTree(TreeNode.of(1, 2), TreeNode.of(1, null, 2))).isFalse();
    assertThat(newSolution().isSameTree(TreeNode.of(1, 2, 3, 4), TreeNode.of(1, 2, 3))).isFalse();
  }
}
