package dev.kata.prod;

import java.util.List;
import java.util.Objects;

/**
 * Kata 29. Построение иерархического дерева из плоского списка за O(N).
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Сервис каталога получает из БД плоский список категорий: {@code id}, {@code parentId}, {@code
 * name}, {@code sortOrder}. Нужно собрать лес деревьев для отдачи в API. Список приходит в
 * произвольном порядке, содержит сотни тысяч узлов, глубина может достигать десятков тысяч.
 * Рекурсия здесь недопустима: {@link StackOverflowError} уронил бы процесс, а не вернул бы 500.
 *
 * <h2>Правила</h2>
 *
 * <ol>
 *   <li><b>Валидация</b> — {@code id} и {@code name} не {@code null}/не пустые; {@code parentId}
 *       либо {@code null} (корень), либо непустой; {@code sortOrder} — любой {@code int}.
 *   <li><b>Дубликаты id</b> — {@link IllegalArgumentException}: неоднозначность опаснее, чем
 *       «возьмём первый».
 *   <li><b>Сироты</b> — узел ссылается на отсутствующего родителя. Политика задаётся {@link
 *       OrphanHandling}:
 *       <ul>
 *         <li>{@link OrphanHandling#THROW} — {@link OrphanNodeException} с id узла и родителя;
 *         <li>{@link OrphanHandling#PROMOTE_TO_ROOT} — узел становится корнем, его поддерево
 *             сохраняется.
 *       </ul>
 *   <li><b>Циклы</b> — {@link CyclicHierarchyException} с путём вида {@code [a, b, c, a]}. Цикл
 *       определяется по указателям на родителя, без рекурсии.
 *   <li><b>Порядок</b> — дети каждого узла и корни леса сортируются по {@code sortOrder} по
 *       возрастанию, при равенстве — по {@code id} лексикографически. Результат детерминирован и не
 *       зависит от порядка входа.
 * </ol>
 *
 * <h2>Сложность</h2>
 *
 * <ul>
 *   <li>Индексация и линковка — O(N); одна сортировка входа — O(N log N), суммарно O(N log N).
 *   <li>Память — O(N) на индексы, детей и результат.
 *   <li>Глубина не влияет на стек: обходы итеративные.
 * </ul>
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>Пустой список — пустой неизменяемый лес.
 *   <li>Один корень без детей; несколько корней; глубокая цепочка на 100 000 узлов.
 *   <li>Цикл из одного узла ({@code parentId == id}) — путь {@code [a, a]}.
 *   <li>Отменённый/пропущенный родитель при {@code PROMOTE_TO_ROOT} не ломает поддерево.
 * </ul>
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.prod.Kata29_HierarchicalTreeBuilderSolution}.
 * Тесты: {@code Kata29_HierarchicalTreeBuilderTest}.
 */
public class Kata29_HierarchicalTreeBuilder {

  /** Узел плоского списка. {@code parentId == null} означает корень. */
  public record FlatNode(String id, String parentId, String name, int sortOrder) {
    public FlatNode {
      requireText(id, "id");
      if (parentId != null && parentId.isBlank()) {
        throw new IllegalArgumentException("parentId must be null or non-blank");
      }
      requireText(name, "name");
    }

    private static void requireText(String value, String field) {
      Objects.requireNonNull(value, field);
      if (value.isBlank()) {
        throw new IllegalArgumentException(field + " must not be blank");
      }
    }
  }

  /** Узел готового дерева. Список детей неизменяем и уже отсортирован. */
  public record TreeNode(String id, String name, int sortOrder, List<TreeNode> children) {
    public TreeNode {
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(name, "name");
      children = List.copyOf(Objects.requireNonNull(children, "children"));
    }
  }

  /** Что делать с узлом, чей родитель отсутствует во входном списке. */
  public enum OrphanHandling {
    THROW,
    PROMOTE_TO_ROOT
  }

  /** Узел ссылается на отсутствующего родителя. */
  public static class OrphanNodeException extends IllegalArgumentException {
    private final String orphanId;
    private final String missingParentId;

    public OrphanNodeException(String orphanId, String missingParentId) {
      super("Node '" + orphanId + "' references missing parent '" + missingParentId + "'");
      this.orphanId = orphanId;
      this.missingParentId = missingParentId;
    }

    public String orphanId() {
      return orphanId;
    }

    public String missingParentId() {
      return missingParentId;
    }
  }

  /** В графе родителей обнаружен цикл; {@link #cyclePath()} начинается и заканчивается одним id. */
  public static class CyclicHierarchyException extends IllegalStateException {
    private final List<String> cyclePath;

    public CyclicHierarchyException(List<String> cyclePath) {
      super("Cycle detected: " + String.join(" -> ", cyclePath));
      this.cyclePath = List.copyOf(cyclePath);
    }

    public List<String> cyclePath() {
      return cyclePath;
    }
  }

  /**
   * Строит лес деревьев.
   *
   * @param nodes плоский список узлов в произвольном порядке
   * @param handling политика для отсутствующих родителей
   * @return отсортированный неизменяемый список корней
   * @throws IllegalArgumentException при дубликате id или невалидном узле
   * @throws OrphanNodeException при {@link OrphanHandling#THROW} и отсутствующем родителе
   * @throws CyclicHierarchyException при цикле в указателях на родителя
   */
  public List<TreeNode> build(List<FlatNode> nodes, OrphanHandling handling) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
