package dev.kata.warmup.week4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.warmup.week4.Warmup4_08_BinaryTreeInorderTraversalSolution;
import dev.kata.warmup.common.TreeNode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 4.8: Binary Tree Inorder Traversal")
class Warmup4_08_BinaryTreeInorderTraversalTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup4_08_BinaryTreeInorderTraversal newSolution() {
    return USE_SOLUTION
        ? new Warmup4_08_BinaryTreeInorderTraversalSolution()
        : new Warmup4_08_BinaryTreeInorderTraversal();
  }

  @Test
  @DisplayName("Классический пример с правым поддеревом")
  void classicExample() {
    assertThat(newSolution().inorderTraversal(TreeNode.of(1, null, 2, 3))).containsExactly(1, 3, 2);
  }

  @Test
  @DisplayName("Пустое дерево и один узел")
  void emptyAndSingle() {
    assertThat(newSolution().inorderTraversal(null)).isEmpty();
    assertThat(newSolution().inorderTraversal(TreeNode.of(1))).containsExactly(1);
  }

  @Test
  @DisplayName("Полное дерево")
  void fullTree() {
    assertThat(newSolution().inorderTraversal(TreeNode.of(1, 2, 3, 4, 5, 6, 7)))
        .containsExactly(4, 2, 5, 1, 6, 3, 7);
  }

  @Test
  @DisplayName("BST даёт отсортированный порядок")
  void bstSorted() {
    assertThat(newSolution().inorderTraversal(TreeNode.of(5, 3, 8, 1, 4, 7, 9)))
        .containsExactly(1, 3, 4, 5, 7, 8, 9);
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<Integer> result = newSolution().inorderTraversal(TreeNode.of(1));
    assertThatThrownBy(() -> result.add(2)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Вырожденное дерево на 10 000 узлов не переполняет стек")
  void deepTreeIterative() {
    int size = 10_000;
    TreeNode root = new TreeNode(0);
    TreeNode current = root;
    for (int i = 1; i < size; i++) {
      current.right = new TreeNode(i);
      current = current.right;
    }

    List<Integer> result = newSolution().inorderTraversal(root);

    assertThat(result).hasSize(size);
    assertThat(result.get(0)).isZero();
    assertThat(result.get(size - 1)).isEqualTo(size - 1);
  }
}
