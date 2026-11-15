package dev.kata.warmup.week4;

import dev.kata.warmup.common.TreeNode;

/**
 * Warmup 4.6 — Same Tree (LeetCode #100).
 *
 * <h2>Условие</h2>
 *
 * <p>Проверить, идентичны ли два бинарных дерева: одинаковая структура и одинаковые значения в
 * соответствующих узлах. Пустые деревья считаются одинаковыми.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * [1,2,3] и [1,2,3]   → true
 * [1,2]   и [1,null,2] → false
 * []      и []         → true
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ число узлов каждого дерева ≤ 100;
 *   <li>-10⁴ ≤ {@code node.val} ≤ 10⁴.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(min(n, m)) — обход прекращается на первом расхождении. Память O(h) на стек рекурсии.
 * Сравнивать level-order списки было бы дороже и потребовало бы дополнительной памяти.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week4.Warmup4_06_SameTreeSolution}.
 */
public class Warmup4_06_SameTree {

  /** Проверяет структурное и значений равенство двух деревьев. */
  public boolean isSameTree(TreeNode p, TreeNode q) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
