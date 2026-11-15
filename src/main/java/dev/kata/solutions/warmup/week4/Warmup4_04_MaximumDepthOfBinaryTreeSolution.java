package dev.kata.solutions.warmup.week4;

import dev.kata.warmup.common.TreeNode;
import dev.kata.warmup.week4.Warmup4_04_MaximumDepthOfBinaryTree;

/**
 * Эталон Warmup 4.4.
 *
 * <p>Рекуррентность очевидна: глубина дерева — 1 плюс максимум из глубин детей. База — пустое
 * дерево даёт 0. Каждый узел обрабатывается ровно один раз, результат не зависит от значений,
 * только от формы.
 */
public final class Warmup4_04_MaximumDepthOfBinaryTreeSolution
    extends Warmup4_04_MaximumDepthOfBinaryTree {

  @Override
  public int maxDepth(TreeNode root) {
    if (root == null) {
      return 0;
    }
    return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
  }
}
