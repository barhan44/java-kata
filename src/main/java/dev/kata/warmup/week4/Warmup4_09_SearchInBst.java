package dev.kata.warmup.week4;

import dev.kata.warmup.common.TreeNode;

/**
 * Warmup 4.9 — Search in a Binary Search Tree (LeetCode #700).
 *
 * <h2>Условие</h2>
 *
 * <p>В бинарном дереве поиска найти узел со значением {@code val} и вернуть поддерево с этим
 * корнем. Если значения нет, вернуть {@code null}. Гарантируется, что значения в BST уникальны.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * searchBST([4,2,7,1,3], 2) → [2,1,3]
 * searchBST([4,2,7,1,3], 5) → null
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ число узлов ≤ 5000;
 *   <li>1 ≤ {@code node.val} ≤ 10⁷;
 *   <li>дерево является корректным BST.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(h), память O(1) итеративно, где h — высота дерева: на каждом шаге отбрасываем половину
 * поддерева. Рекурсия заняла бы O(h) стека.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week4.Warmup4_09_SearchInBstSolution}.
 */
public class Warmup4_09_SearchInBst {

  /** Возвращает поддерево с корнем {@code val} или {@code null}. */
  public TreeNode searchBST(TreeNode root, int val) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
