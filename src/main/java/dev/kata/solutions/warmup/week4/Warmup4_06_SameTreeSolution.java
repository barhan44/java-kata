package dev.kata.solutions.warmup.week4;

import dev.kata.warmup.common.TreeNode;
import dev.kata.warmup.week4.Warmup4_06_SameTree;

/**
 * Эталон Warmup 4.6.
 *
 * <p>Три случая: оба узла {@code null} — совпали; один {@code null} — структуры различны; значения
 * различаются — деревья различны. Иначе рекурсивно сравниваем левые и правые поддеревья. Короткое
 * замыкание {@code &&} прекращает обход при первом несовпадении.
 */
public final class Warmup4_06_SameTreeSolution extends Warmup4_06_SameTree {

  @Override
  public boolean isSameTree(TreeNode p, TreeNode q) {
    if (p == null && q == null) {
      return true;
    }
    if (p == null || q == null) {
      return false;
    }
    return p.val == q.val && isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
  }
}
