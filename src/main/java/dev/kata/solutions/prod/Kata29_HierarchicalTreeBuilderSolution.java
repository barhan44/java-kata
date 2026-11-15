package dev.kata.solutions.prod;

import dev.kata.prod.Kata29_HierarchicalTreeBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Эталонное решение Kata 29.
 *
 * <h2>Почему сортировка входа решает порядок детей</h2>
 *
 * <p>Вместо сортировки каждого списка детей отдельно входной список один раз сортируется по {@code
 * (sortOrder, id)}. Затем дети раскладываются по родителям в этом порядке — списки детей и корней
 * автоматически оказываются отсортированными. Одна сортировка O(N log N) вместо сортировки на
 * каждом уровне; линковка после неё — O(N).
 *
 * <h2>Почему обходы итеративные</h2>
 *
 * <p>Дерево глубиной 100 000 при рекурсивном построении гарантированно исчерпает стек. Цикл ищется
 * обходом указателей на родителя с трёхцветной раскраской: серый узел, встреченный повторно,
 * означает цикл, а сохранённая позиция даёт путь от входа в цикл. Построение идёт в порядке,
 * обратном BFS от корней: к моменту создания узла все его дети уже созданы.
 *
 * <h2>Сироты</h2>
 *
 * <p>Родитель, которого нет в индексе, либо сразу приводит к {@link OrphanNodeException} ({@code
 * THROW}), либо подменяется на {@code null} ({@code PROMOTE_TO_ROOT}). Во втором случае узел
 * попадает в корни, а его собственные дети остаются привязанными к нему — поддерево цело.
 *
 * <h2>Сложность</h2>
 *
 * <p>Время O(N log N) из-за сортировки, память O(N). Все структуры — {@link HashMap}/{@link
 * ArrayList}; красно-чёрные деревья здесь не нужны.
 */
public final class Kata29_HierarchicalTreeBuilderSolution extends Kata29_HierarchicalTreeBuilder {

  @Override
  public List<TreeNode> build(List<FlatNode> nodes, OrphanHandling handling) {
    Objects.requireNonNull(nodes, "nodes");
    Objects.requireNonNull(handling, "handling");

    Map<String, FlatNode> byId = new HashMap<>(capacity(nodes.size()));
    List<String> inputOrder = new ArrayList<>(nodes.size());
    for (FlatNode node : nodes) {
      Objects.requireNonNull(node, "node");
      if (byId.putIfAbsent(node.id(), node) != null) {
        throw new IllegalArgumentException("Duplicate node id: " + node.id());
      }
      inputOrder.add(node.id());
    }

    // Эффективный родитель: null — корень. Отсутствующий родитель либо ошибка, либо корень.
    Map<String, String> parentOf = new HashMap<>(capacity(byId.size()));
    for (String id : inputOrder) {
      String parentId = byId.get(id).parentId();
      if (parentId == null) {
        parentOf.put(id, null);
      } else if (byId.containsKey(parentId)) {
        parentOf.put(id, parentId);
      } else if (handling == OrphanHandling.THROW) {
        throw new OrphanNodeException(id, parentId);
      } else {
        parentOf.put(id, null);
      }
    }

    detectCycles(inputOrder, parentOf);

    // Одна сортировка задаёт порядок корней и детей во всём лесу.
    List<FlatNode> sorted = new ArrayList<>(nodes);
    sorted.sort(Comparator.comparingInt(FlatNode::sortOrder).thenComparing(FlatNode::id));

    List<FlatNode> roots = new ArrayList<>();
    Map<String, List<String>> childIds = new HashMap<>(capacity(byId.size()));
    for (FlatNode node : sorted) {
      String parentId = parentOf.get(node.id());
      if (parentId == null) {
        roots.add(node);
      } else {
        childIds.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(node.id());
      }
    }

    // BFS от корней: родитель всегда попадает в порядок раньше своих детей.
    List<String> bfsOrder = new ArrayList<>(byId.size());
    ArrayDeque<String> queue = new ArrayDeque<>(roots.size());
    for (FlatNode root : roots) {
      queue.add(root.id());
    }
    while (!queue.isEmpty()) {
      String id = queue.poll();
      bfsOrder.add(id);
      for (String childId : childIds.getOrDefault(id, List.of())) {
        queue.add(childId);
      }
    }

    // Обратный BFS: дети созданы раньше родителя. Рекурсии нет — глубина не важна.
    Map<String, TreeNode> built = new HashMap<>(capacity(bfsOrder.size()));
    for (int i = bfsOrder.size() - 1; i >= 0; i--) {
      String id = bfsOrder.get(i);
      FlatNode flat = byId.get(id);
      List<TreeNode> children = new ArrayList<>();
      for (String childId : childIds.getOrDefault(id, List.of())) {
        children.add(built.get(childId));
      }
      built.put(id, new TreeNode(flat.id(), flat.name(), flat.sortOrder(), children));
    }

    List<TreeNode> forest = new ArrayList<>(roots.size());
    for (FlatNode root : roots) {
      forest.add(built.get(root.id()));
    }
    return List.copyOf(forest);
  }

  /**
   * Трёхцветный обход указателей на родителя.
   *
   * <p>0 — не посещён, 1 — в текущем пути (серый), 2 — обработан (чёрный). Если при подъёме мы
   * встретили серый узел, он уже есть в текущем пути: вырезаем путь от него до конца и замыкаем.
   * Поскольку после каждого старта весь путь помечается чёрным, серый узел всегда принадлежит
   * текущему обходу, а каждый узел посещается не более одного раза.
   */
  private static void detectCycles(List<String> inputOrder, Map<String, String> parentOf) {
    Map<String, Integer> state = new HashMap<>(capacity(inputOrder.size()));
    List<String> path = new ArrayList<>();
    Map<String, Integer> positionInPath = new HashMap<>(capacity(inputOrder.size()));

    for (String start : inputOrder) {
      if (state.getOrDefault(start, 0) != 0) {
        continue;
      }
      path.clear();
      positionInPath.clear();

      String current = start;
      while (current != null && state.getOrDefault(current, 0) == 0) {
        state.put(current, 1);
        positionInPath.put(current, path.size());
        path.add(current);
        current = parentOf.get(current);
      }

      if (current != null && state.get(current) == 1) {
        List<String> cycle =
            new ArrayList<>(path.subList(positionInPath.get(current), path.size()));
        cycle.add(current);
        throw new CyclicHierarchyException(cycle);
      }

      for (String id : path) {
        state.put(id, 2);
      }
    }
  }

  /** Ёмкость без перестроек при 0.75 load factor, с защитой от переполнения. */
  private static int capacity(int expectedSize) {
    if (expectedSize < 3) {
      return expectedSize + 1;
    }
    return (int) Math.ceil(expectedSize / 0.75);
  }
}
