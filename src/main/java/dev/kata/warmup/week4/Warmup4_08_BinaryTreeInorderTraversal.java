package dev.kata.warmup.week4;

import dev.kata.warmup.common.TreeNode;
import java.util.List;

/**
 * Warmup 4.8 — Binary Tree Inorder Traversal (LeetCode #94).
 *
 * <h2>Условие</h2>
 *
 * <p>Вернуть значения узлов в порядке in-order: левое поддерево, корень, правое поддерево. Обход
 * должен быть итеративным, с явным стеком на {@code ArrayDeque}; устаревший {@code java.util.Stack}
 * и рекурсию использовать нельзя.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * [1,null,2,3] → [1,3,2]
 * []           → []
 * [1]          → [1]
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
 * <p>Время O(n), память O(h) на стек, где h — высота дерева. Итеративный вариант не рискует {@link
 * StackOverflowError} на вырожденном дереве; {@code ArrayDeque} быстрее синхронизированного {@code
 * Stack}.
 *
 * <p>Реализацию см. в {@code
 * dev.kata.solutions.warmup.week4.Warmup4_08_BinaryTreeInorderTraversalSolution}.
 */
public class Warmup4_08_BinaryTreeInorderTraversal {

  /** Возвращает in-order обход дерева. */
  public List<Integer> inorderTraversal(TreeNode root) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
