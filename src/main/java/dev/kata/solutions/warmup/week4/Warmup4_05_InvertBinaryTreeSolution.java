package dev.kata.solutions.warmup.week4;

import dev.kata.warmup.common.TreeNode;
import dev.kata.warmup.week4.Warmup4_05_InvertBinaryTree;

/**
 * Эталон Warmup 4.5.
 *
 * <p>Сначала рекурсивно инвертируем поддеревья, затем меняем ссылки местами. Порядок операций не
 * важен: можно и сразу поменять {@code left/right} и рекурсивно обойти новые дети. Инвариант —
 * после вызова поддерево с корнем {@code root} зеркально исходному.
 */
public final class Warmup4_05_InvertBinaryTreeSolution extends Warmup4_05_InvertBinaryTree {

  @Override
  public TreeNode invertTree(TreeNode root) {
    if (root == null) {
      return null;
    }

    TreeNode invertedLeft = invertTree(root.left);
    TreeNode invertedRight = invertTree(root.right);
    root.left = invertedRight;
    root.right = invertedLeft;
    return root;
  }
}
