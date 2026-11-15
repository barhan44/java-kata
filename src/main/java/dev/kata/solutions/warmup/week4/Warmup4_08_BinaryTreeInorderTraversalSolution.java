package dev.kata.solutions.warmup.week4;

import dev.kata.warmup.common.TreeNode;
import dev.kata.warmup.week4.Warmup4_08_BinaryTreeInorderTraversal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Эталон Warmup 4.8.
 *
 * <p>Итеративный in-order: идём влево, складывая узлы в {@code ArrayDeque}; когда левого ребёнка
 * нет, снимаем верхний узел, добавляем его значение и переходим в правое поддерево. Внешний цикл
 * продолжается, пока есть текущий узел или стек не пуст. Каждый узел кладётся и снимается ровно
 * один раз — O(n).
 *
 * <p>{@code ArrayDeque} не синхронизирован и запрещает {@code null}; узлы дерева не {@code null},
 * поэтому подходит идеально. Результат оборачивается {@link List#copyOf} — наружу не утекает
 * мутабельный список.
 */
public final class Warmup4_08_BinaryTreeInorderTraversalSolution
    extends Warmup4_08_BinaryTreeInorderTraversal {

  @Override
  public List<Integer> inorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode current = root;

    while (current != null || !stack.isEmpty()) {
      while (current != null) {
        stack.push(current);
        current = current.left;
      }
      current = stack.pop();
      result.add(current.val);
      current = current.right;
    }
    return List.copyOf(result);
  }
}
