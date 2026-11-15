package dev.kata.warmup.week4;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week4.Warmup4_05_InvertBinaryTreeSolution;
import dev.kata.warmup.common.TreeNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 4.5: Invert Binary Tree")
class Warmup4_05_InvertBinaryTreeTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup4_05_InvertBinaryTree newSolution() {
    return USE_SOLUTION
        ? new Warmup4_05_InvertBinaryTreeSolution()
        : new Warmup4_05_InvertBinaryTree();
  }

  @Test
  @DisplayName("Классическое симметричное дерево")
  void classicTree() {
    TreeNode inverted = newSolution().invertTree(TreeNode.of(4, 2, 7, 1, 3, 6, 9));
    assertThat(TreeNode.toList(inverted)).containsExactly(4, 7, 2, 9, 6, 3, 1);
  }

  @Test
  @DisplayName("Пустое дерево и один узел")
  void emptyAndSingle() {
    assertThat(newSolution().invertTree(null)).isNull();

    TreeNode single = newSolution().invertTree(TreeNode.of(1));
    assertThat(TreeNode.toList(single)).containsExactly(1);
  }

  @Test
  @DisplayName("Неполное дерево сохраняет структуру")
  void asymmetricTree() {
    TreeNode inverted = newSolution().invertTree(TreeNode.of(1, 2, null, 3));
    assertThat(TreeNode.toList(inverted)).containsExactly(1, null, 2, null, 3);
  }

  @Test
  @DisplayName("Двойная инверсия возвращает исходную форму")
  void involution() {
    TreeNode original = TreeNode.of(5, 3, 8, 1, null, 7, 9);
    TreeNode twice = newSolution().invertTree(newSolution().invertTree(original));
    assertThat(TreeNode.toList(twice)).isEqualTo(TreeNode.toList(original));
  }
}
