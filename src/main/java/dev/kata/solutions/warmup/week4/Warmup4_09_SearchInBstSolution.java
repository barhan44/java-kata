package dev.kata.solutions.warmup.week4;

import dev.kata.warmup.common.TreeNode;
import dev.kata.warmup.week4.Warmup4_09_SearchInBst;

/**
 * Эталон Warmup 4.9.
 *
 * <p>Итеративный спуск по BST: если {@code val} меньше значения узла — идём влево, больше — вправо,
 * равно — возвращаем текущий узел. Спуск прекращается на {@code null}, что означает отсутствие
 * значения. Память O(1), стек не растёт.
 */
public final class Warmup4_09_SearchInBstSolution extends Warmup4_09_SearchInBst {

  @Override
  public TreeNode searchBST(TreeNode root, int val) {
    TreeNode current = root;
    while (current != null) {
      if (val == current.val) {
        return current;
      }
      current = val < current.val ? current.left : current.right;
    }
    return null;
  }
}
