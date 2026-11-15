package dev.kata.prod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.prod.Kata29_HierarchicalTreeBuilder.CyclicHierarchyException;
import dev.kata.prod.Kata29_HierarchicalTreeBuilder.FlatNode;
import dev.kata.prod.Kata29_HierarchicalTreeBuilder.OrphanHandling;
import dev.kata.prod.Kata29_HierarchicalTreeBuilder.OrphanNodeException;
import dev.kata.prod.Kata29_HierarchicalTreeBuilder.TreeNode;
import dev.kata.solutions.prod.Kata29_HierarchicalTreeBuilderSolution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 29.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata29* -DuseSolution=true</pre>
 */
@DisplayName("Kata 29: иерархия из плоского списка")
class Kata29_HierarchicalTreeBuilderTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata29_HierarchicalTreeBuilder newBuilder() {
    return USE_SOLUTION
        ? new Kata29_HierarchicalTreeBuilderSolution()
        : new Kata29_HierarchicalTreeBuilder();
  }

  @Test
  @DisplayName("Строит лес и сортирует корни и детей по sortOrder, затем по id")
  void buildsAndSortsForest() {
    List<FlatNode> input =
        List.of(
            node("a", "r1", "A", 2),
            node("r2", "null", "R2", 5),
            node("g", "a", "G", 0),
            node("r1", "null", "R1", 10),
            node("b", "r1", "B", 2));

    List<TreeNode> forest = newBuilder().build(input, OrphanHandling.THROW);

    assertThat(forest).extracting(TreeNode::id).containsExactly("r2", "r1");
    TreeNode r1 = forest.get(1);
    assertThat(r1.children()).extracting(TreeNode::id).containsExactly("a", "b");
    assertThat(r1.children().get(0).children()).extracting(TreeNode::id).containsExactly("g");
    assertThat(r1.children().get(1).children()).isEmpty();
    assertThat(r1.name()).isEqualTo("R1");
  }

  @Test
  @DisplayName("Корни с одинаковым sortOrder сортируются по id")
  void rootsWithSameOrderSortedById() {
    List<TreeNode> forest =
        newBuilder()
            .build(
                List.of(node("root-b", "null", "B", 0), node("root-a", "null", "A", 0)),
                OrphanHandling.THROW);

    assertThat(forest).extracting(TreeNode::id).containsExactly("root-a", "root-b");
  }

  @Test
  @DisplayName("Порядок входа не влияет на результат")
  void inputOrderDoesNotMatter() {
    List<FlatNode> input =
        List.of(
            node("r1", "null", "R1", 1),
            node("a", "r1", "A", 2),
            node("b", "r1", "B", 1),
            node("g", "a", "G", 0));
    List<FlatNode> reversed = new ArrayList<>(input);
    java.util.Collections.reverse(reversed);

    TreeNode expected = newBuilder().build(input, OrphanHandling.THROW).get(0);
    TreeNode actual = newBuilder().build(reversed, OrphanHandling.THROW).get(0);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("Дубликат id — IllegalArgumentException")
  void duplicateIdRejected() {
    assertThatThrownBy(
            () ->
                newBuilder()
                    .build(
                        List.of(node("x", "null", "First", 0), node("x", "null", "Second", 1)),
                        OrphanHandling.THROW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate node id: x");
  }

  @Test
  @DisplayName("Сирота при THROW сообщает id узла и отсутствующего родителя")
  void orphanThrows() {
    assertThatThrownBy(
            () ->
                newBuilder()
                    .build(
                        List.of(
                            node("root", "null", "Root", 0), node("orphan", "ghost", "Orphan", 1)),
                        OrphanHandling.THROW))
        .isInstanceOf(OrphanNodeException.class)
        .satisfies(
            error -> {
              OrphanNodeException orphan = (OrphanNodeException) error;
              assertThat(orphan.orphanId()).isEqualTo("orphan");
              assertThat(orphan.missingParentId()).isEqualTo("ghost");
            })
        .hasMessageContaining("ghost");
  }

  @Test
  @DisplayName("Сирота при PROMOTE_TO_ROOT становится корнем и сохраняет поддерево")
  void orphanPromotedToRoot() {
    List<TreeNode> forest =
        newBuilder()
            .build(
                List.of(
                    node("real", "null", "Real", 5),
                    node("orphan", "ghost", "Orphan", 1),
                    node("child", "orphan", "Child", 0)),
                OrphanHandling.PROMOTE_TO_ROOT);

    assertThat(forest).extracting(TreeNode::id).containsExactly("orphan", "real");
    assertThat(forest.get(0).children()).extracting(TreeNode::id).containsExactly("child");
  }

  @Test
  @DisplayName("Несколько сирот при PROMOTE_TO_ROOT упорядочены вместе с корнями")
  void multipleOrphansPromoted() {
    List<TreeNode> forest =
        newBuilder()
            .build(
                List.of(
                    node("o2", "missing", "O2", 2),
                    node("root", "null", "Root", 3),
                    node("o1", "missing", "O1", 1)),
                OrphanHandling.PROMOTE_TO_ROOT);

    assertThat(forest).extracting(TreeNode::id).containsExactly("o1", "o2", "root");
  }

  @Test
  @DisplayName("Цикл A → B → A обнаруживается с путём")
  void directCycle() {
    assertThatThrownBy(
            () ->
                newBuilder()
                    .build(
                        List.of(node("a", "b", "A", 0), node("b", "a", "B", 0)),
                        OrphanHandling.THROW))
        .isInstanceOf(CyclicHierarchyException.class)
        .satisfies(
            error -> {
              List<String> path = ((CyclicHierarchyException) error).cyclePath();
              assertThat(path).containsExactly("a", "b", "a");
              assertThatThrownBy(() -> path.add("x"))
                  .isInstanceOf(UnsupportedOperationException.class);
            })
        .hasMessageContaining("a -> b -> a");
  }

  @Test
  @DisplayName("Узел, ссылающийся сам на себя, — цикл [a, a]")
  void selfCycle() {
    assertThatThrownBy(
            () -> newBuilder().build(List.of(node("a", "a", "A", 0)), OrphanHandling.THROW))
        .isInstanceOf(CyclicHierarchyException.class)
        .satisfies(
            error ->
                assertThat(((CyclicHierarchyException) error).cyclePath())
                    .containsExactly("a", "a"));
  }

  @Test
  @DisplayName("Цикл в середине дерева: путь не включает ведущий хвост")
  void cycleInMiddle() {
    assertThatThrownBy(
            () ->
                newBuilder()
                    .build(
                        List.of(
                            node("tail", "a", "Tail", 0),
                            node("a", "b", "A", 1),
                            node("b", "a", "B", 2)),
                        OrphanHandling.PROMOTE_TO_ROOT))
        .isInstanceOf(CyclicHierarchyException.class)
        .satisfies(
            error ->
                assertThat(((CyclicHierarchyException) error).cyclePath())
                    .containsExactly("a", "b", "a"));
  }

  @Test
  @DisplayName("Цикл обнаруживается и при наличии сироты, которую промоутят")
  void cycleDetectedAlongsidePromotedOrphan() {
    assertThatThrownBy(
            () ->
                newBuilder()
                    .build(
                        List.of(
                            node("orphan", "ghost", "Orphan", 0),
                            node("x", "y", "X", 1),
                            node("y", "x", "Y", 2)),
                        OrphanHandling.PROMOTE_TO_ROOT))
        .isInstanceOf(CyclicHierarchyException.class);
  }

  @Test
  @DisplayName("Глубокая цепочка на 100 000 узлов не переполняет стек")
  void deepChainDoesNotOverflow() {
    int depth = 100_000;
    List<FlatNode> chain = new ArrayList<>(depth);
    for (int i = depth - 1; i >= 0; i--) {
      String id = "n" + i;
      String parentId = i == 0 ? null : "n" + (i - 1);
      chain.add(node(id, parentId, "Node " + i, 0));
    }

    List<TreeNode> forest = newBuilder().build(chain, OrphanHandling.THROW);

    assertThat(forest).hasSize(1);
    int actualDepth = 1;
    TreeNode current = forest.get(0);
    while (!current.children().isEmpty()) {
      current = current.children().get(0);
      actualDepth++;
    }
    assertThat(actualDepth).isEqualTo(depth);
    assertThat(current.id()).isEqualTo("n" + (depth - 1));
  }

  @Test
  @DisplayName("Широкий уровень: 1000 детей сортируются детерминированно")
  void wideLevelSorted() {
    List<FlatNode> input = new ArrayList<>();
    input.add(node("root", "null", "Root", 0));
    for (int i = 1000; i > 0; i--) {
      input.add(node(String.format("c%04d", i), "root", "Child " + i, 0));
    }

    List<TreeNode> children = newBuilder().build(input, OrphanHandling.THROW).get(0).children();

    assertThat(children).hasSize(1000);
    assertThat(children.get(0).id()).isEqualTo("c0001");
    assertThat(children.get(999).id()).isEqualTo("c1000");
  }

  @Test
  @DisplayName("Пустой список — пустой неизменяемый лес")
  void emptyInput() {
    List<TreeNode> forest = newBuilder().build(List.of(), OrphanHandling.THROW);

    assertThat(forest).isEmpty();
    assertThatThrownBy(() -> forest.add(null)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Списки детей и корней неизменяемы")
  void resultImmutable() {
    List<TreeNode> forest =
        newBuilder()
            .build(
                List.of(node("root", "null", "Root", 0), node("child", "root", "Child", 0)),
                OrphanHandling.THROW);

    assertThatThrownBy(() -> forest.get(0).children().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> forest.add(null)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("null-аргументы и null-элемент списка")
  void nullArguments() {
    assertThatThrownBy(() -> newBuilder().build(null, OrphanHandling.THROW))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newBuilder().build(List.of(), null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> newBuilder().build(Arrays.asList((FlatNode) null), OrphanHandling.THROW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Валидация FlatNode и TreeNode")
  void validation() {
    assertThatThrownBy(() -> node(null, "p", "N", 0)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> node(" ", "p", "N", 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> node("id", " ", "N", 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> node("id", "p", null, 0)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> node("id", "p", "  ", 0)).isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new TreeNode(null, "N", 0, List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new TreeNode("id", null, 0, List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new TreeNode("id", "N", 0, null))
        .isInstanceOf(NullPointerException.class);
  }

  private static FlatNode node(String id, String parentId, String name, int sortOrder) {
    return new FlatNode(id, "null".equals(parentId) ? null : parentId, name, sortOrder);
  }
}
