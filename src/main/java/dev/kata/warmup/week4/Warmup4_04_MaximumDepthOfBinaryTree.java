package dev.kata.warmup.week4;

import dev.kata.warmup.common.TreeNode;

/**
 * Warmup 4.4 — Maximum Depth of Binary Tree (LeetCode #104).
 *
 * <h2>Условие</h2>
 *
 * <p>Максимальная глубина дерева — число узлов на самом длинном пути от корня до листа. Пустое
 * дерево имеет глубину 0, дерево из одного узла — 1.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 *       3
 *      / \
 *     9  20
 *       /  \
 *      15   7
 * maxDepth → 3
 *
 * maxDepth([]) → 0
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ число узлов ≤ 10⁴;
 *   <li>-100 ≤ {@code node.val} ≤ 100.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n) — каждый узел посещается один раз. Память O(h) на стек рекурсии, где h — высота
 * дерева: O(log n) для сбалансированного и O(n) для вырожденного. Итеративный BFS потребовал бы
 * O(w) памяти на очередь, где w — ширина.
 *
 * <p>Реализацию см. в {@code
 * dev.kata.solutions.warmup.week4.Warmup4_04_MaximumDepthOfBinaryTreeSolution}.
 */
public class Warmup4_04_MaximumDepthOfBinaryTree {

  /** Возвращает максимальную глубину дерева. */
  public int maxDepth(TreeNode root) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
