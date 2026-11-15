package dev.kata.warmup.week4;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week4.Warmup4_09_SearchInBstSolution;
import dev.kata.warmup.common.TreeNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 4.9: Search in a Binary Search Tree")
class Warmup4_09_SearchInBstTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup4_09_SearchInBst newSolution() {
    return USE_SOLUTION ? new Warmup4_09_SearchInBstSolution() : new Warmup4_09_SearchInBst();
  }

  @Test
  @DisplayName("Классический пример: найден внутренний узел")
  void findsInternalNode() {
    TreeNode root = TreeNode.of(4, 2, 7, 1, 3);
    TreeNode found = newSolution().searchBST(root, 2);
    assertThat(TreeNode.toList(found)).containsExactly(2, 1, 3);
  }

  @Test
  @DisplayName("Найден корень")
  void findsRoot() {
    TreeNode root = TreeNode.of(4, 2, 7, 1, 3);
    assertThat(newSolution().searchBST(root, 4)).isSameAs(root);
  }

  @Test
  @DisplayName("Найден лист")
  void findsLeaf() {
    TreeNode root = TreeNode.of(4, 2, 7, 1, 3);
    TreeNode found = newSolution().searchBST(root, 1);
    assertThat(TreeNode.toList(found)).containsExactly(1);
  }

  @Test
  @DisplayName("Значение отсутствует")
  void missingValue() {
    TreeNode root = TreeNode.of(4, 2, 7, 1, 3);
    assertThat(newSolution().searchBST(root, 5)).isNull();
    assertThat(newSolution().searchBST(root, 0)).isNull();
    assertThat(newSolution().searchBST(root, 100)).isNull();
  }

  @Test
  @DisplayName("Пустое дерево")
  void emptyTree() {
    assertThat(newSolution().searchBST(null, 1)).isNull();
  }

  @Test
  @DisplayName("Спуск по левой и правой веткам")
  void descendsBothDirections() {
    TreeNode root = TreeNode.of(8, 3, 10, 1, 6, null, 14, null, null, 4, 7, 13);
    assertThat(TreeNode.toList(newSolution().searchBST(root, 1))).containsExactly(1);
    assertThat(TreeNode.toList(newSolution().searchBST(root, 7))).containsExactly(7);
    assertThat(TreeNode.toList(newSolution().searchBST(root, 13))).containsExactly(13);
    assertThat(newSolution().searchBST(root, 9)).isNull();
  }
}
