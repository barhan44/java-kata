package dev.kata.solutions.warmup.week4;

import dev.kata.warmup.common.TreeNode;
import dev.kata.warmup.week4.Warmup4_07_SymmetricTree;

/**
 * Эталон Warmup 4.7.
 *
 * <p>Пустое дерево симметрично. Для непустого сравниваем два поддерева как зеркала: {@code
 * isMirror(left, right)}. Базовые случаи: оба {@code null} — зеркала; один {@code null} — нет.
 * Значения должны совпадать, а дети сравниваются перекрёстно: {@code left.left ↔ right.right} и
 * {@code left.right ↔ right.left}.
 */
public final class Warmup4_07_SymmetricTreeSolution extends Warmup4_07_SymmetricTree {

  @Override
  public boolean isSymmetric(TreeNode root) {
    return root == null || isMirror(root.left, root.right);
  }

  private static boolean isMirror(TreeNode left, TreeNode right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.val == right.val
        && isMirror(left.left, right.right)
        && isMirror(left.right, right.left);
  }
}
