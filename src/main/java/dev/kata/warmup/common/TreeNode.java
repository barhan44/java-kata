package dev.kata.warmup.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Узел бинарного дерева в стиле LeetCode.
 *
 * <p>{@link #of(Integer...)} принимает level-order массив, где {@code null} обозначает
 * отсутствующего ребёнка; дети {@code null}-узлов в массиве не описываются. {@link #toList()}
 * выполняет обратное преобразование (с обрезкой хвостовых {@code null}), поэтому тесты могут
 * сравнивать деревья компактно.
 */
public class TreeNode {

  public int val;
  public TreeNode left;
  public TreeNode right;

  public TreeNode() {}

  public TreeNode(int val) {
    this.val = val;
  }

  public TreeNode(int val, TreeNode left, TreeNode right) {
    this.val = val;
    this.left = left;
    this.right = right;
  }

  /**
   * Строит дерево из LeetCode level-order массива.
   *
   * @param levelOrderValues значения по уровням; {@code null} — отсутствующий ребёнок
   * @return корень или {@code null}, если массив пуст или начинается с {@code null}
   */
  public static TreeNode of(Integer... levelOrderValues) {
    Objects.requireNonNull(levelOrderValues, "levelOrderValues");
    if (levelOrderValues.length == 0 || levelOrderValues[0] == null) {
      return null;
    }

    TreeNode root = new TreeNode(levelOrderValues[0]);
    Deque<TreeNode> queue = new ArrayDeque<>();
    queue.add(root);

    int index = 1;
    while (!queue.isEmpty() && index < levelOrderValues.length) {
      TreeNode current = queue.poll();

      if (index < levelOrderValues.length) {
        Integer leftValue = levelOrderValues[index++];
        if (leftValue != null) {
          current.left = new TreeNode(leftValue);
          queue.add(current.left);
        }
      }

      if (index < levelOrderValues.length) {
        Integer rightValue = levelOrderValues[index++];
        if (rightValue != null) {
          current.right = new TreeNode(rightValue);
          queue.add(current.right);
        }
      }
    }
    return root;
  }

  /** Удобная null-safe перегрузка для тестов. */
  public static List<Integer> toList(TreeNode root) {
    return root == null ? List.of() : root.toList();
  }

  /**
   * Возвращает level-order представление дерева с {@code null} для отсутствующих детей. Хвостовые
   * {@code null} обрезаются, как в выводе LeetCode.
   */
  public List<Integer> toList() {
    List<Integer> result = new ArrayList<>();
    LinkedList<TreeNode> queue = new LinkedList<>();
    queue.add(this);

    while (!queue.isEmpty()) {
      TreeNode node = queue.removeFirst();
      if (node == null) {
        result.add(null);
        continue;
      }
      result.add(node.val);
      queue.addLast(node.left);
      queue.addLast(node.right);
    }

    while (!result.isEmpty() && result.get(result.size() - 1) == null) {
      result.remove(result.size() - 1);
    }
    // List.copyOf запрещает null, а level-order представление использует null для дырок.
    return Collections.unmodifiableList(new ArrayList<>(result));
  }

  @Override
  public String toString() {
    return toList().toString();
  }
}
