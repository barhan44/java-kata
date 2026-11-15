package dev.kata.warmup.week4;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week4.Warmup4_04_MaximumDepthOfBinaryTreeSolution;
import dev.kata.warmup.common.TreeNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 4.4: Maximum Depth of Binary Tree")
class Warmup4_04_MaximumDepthOfBinaryTreeTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup4_04_MaximumDepthOfBinaryTree newSolution() {
    return USE_SOLUTION
        ? new Warmup4_04_MaximumDepthOfBinaryTreeSolution()
        : new Warmup4_04_MaximumDepthOfBinaryTree();
  }

  @Test
  @DisplayName("Классическое дерево глубины 3")
  void classicTree() {
    TreeNode root = TreeNode.of(3, 9, 20, null, null, 15, 7);
    assertThat(newSolution().maxDepth(root)).isEqualTo(3);
  }

  @Test
  @DisplayName("Пустое дерево и один узел")
  void emptyAndSingle() {
    assertThat(newSolution().maxDepth(null)).isZero();
    assertThat(newSolution().maxDepth(TreeNode.of(1))).isEqualTo(1);
  }

  @Test
  @DisplayName("Вырожденное дерево вправо")
  void rightSkewed() {
    assertThat(newSolution().maxDepth(TreeNode.of(1, null, 2, null, 3, null, 4))).isEqualTo(4);
  }

  @Test
  @DisplayName("Вырожденное дерево влево")
  void leftSkewed() {
    assertThat(newSolution().maxDepth(TreeNode.of(1, 2, null, 3, null, 4))).isEqualTo(4);
  }

  @Test
  @DisplayName("Неполное дерево: максимум не по короткой ветке")
  void unbalanced() {
    assertThat(newSolution().maxDepth(TreeNode.of(1, 2, 3, 4))).isEqualTo(3);
  }
}
