package dev.kata.warmup.week4;

import dev.kata.warmup.common.TreeNode;

/**
 * Warmup 4.5 — Invert Binary Tree (LeetCode #226).
 *
 * <h2>Условие</h2>
 *
 * <p>Отразить бинарное дерево относительно вертикальной оси: у каждого узла левое и правое
 * поддеревья меняются местами. Вернуть корень преобразованного дерева.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 *      4                     4
 *    /   \                 /   \
 *   2     7      →        7     2
 *  / \   / \             / \   / \
 * 1   3 6   9           9   6 3   1
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ число узлов ≤ 100;
 *   <li>-100 ≤ {@code node.val} ≤ 100.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(h) на рекурсию. Можно и итеративно через {@code ArrayDeque}: обойти
 * дерево и на каждом узле поменять детей. Рекурсивный вариант короче и читаемее при малой высоте.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week4.Warmup4_05_InvertBinaryTreeSolution}.
 */
public class Warmup4_05_InvertBinaryTree {

  /** Отражает дерево и возвращает его корень. */
  public TreeNode invertTree(TreeNode root) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
