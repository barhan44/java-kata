package dev.kata.warmup.week4;

import dev.kata.warmup.common.TreeNode;

/**
 * Warmup 4.7 — Symmetric Tree (LeetCode #101).
 *
 * <h2>Условие</h2>
 *
 * <p>Проверить, является ли бинарное дерево зеркально симметричным относительно своей вертикальной
 * оси. Симметрия определяется рекурсивно: левое поддерево узла должно быть зеркальным отражением
 * правого.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * [1,2,2,3,4,4,3]         → true
 * [1,2,2,null,3,null,3]   → false
 * []                      → true
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ число узлов ≤ 1000;
 *   <li>-100 ≤ {@code node.val} ≤ 100.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(h) на стек рекурсии. Сравниваем не два одинаковых поддерева, а зеркальные
 * пары: {@code left.left} с {@code right.right} и {@code left.right} с {@code right.left}. Обход в
 * ширину с очередью тоже возможен, но хранит O(w) узлов.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week4.Warmup4_07_SymmetricTreeSolution}.
 */
public class Warmup4_07_SymmetricTree {

  /** Проверяет зеркальную симметрию дерева. */
  public boolean isSymmetric(TreeNode root) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
