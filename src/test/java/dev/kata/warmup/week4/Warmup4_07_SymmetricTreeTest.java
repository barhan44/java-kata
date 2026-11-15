package dev.kata.warmup.week4;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week4.Warmup4_07_SymmetricTreeSolution;
import dev.kata.warmup.common.TreeNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 4.7: Symmetric Tree")
class Warmup4_07_SymmetricTreeTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup4_07_SymmetricTree newSolution() {
    return USE_SOLUTION ? new Warmup4_07_SymmetricTreeSolution() : new Warmup4_07_SymmetricTree();
  }

  @Test
  @DisplayName("Классическое симметричное дерево")
  void symmetricTree() {
    assertThat(newSolution().isSymmetric(TreeNode.of(1, 2, 2, 3, 4, 4, 3))).isTrue();
  }

  @Test
  @DisplayName("Одинаковые значения, но неверная структура")
  void asymmetricStructure() {
    assertThat(newSolution().isSymmetric(TreeNode.of(1, 2, 2, null, 3, null, 3))).isFalse();
  }

  @Test
  @DisplayName("Пустое дерево и один узел симметричны")
  void emptyAndSingle() {
    assertThat(newSolution().isSymmetric(null)).isTrue();
    assertThat(newSolution().isSymmetric(TreeNode.of(1))).isTrue();
  }

  @Test
  @DisplayName("Два одинаковых ребёнка симметричны")
  void twoSameChildren() {
    assertThat(newSolution().isSymmetric(TreeNode.of(1, 2, 2))).isTrue();
    assertThat(newSolution().isSymmetric(TreeNode.of(1, 2, 3))).isFalse();
  }

  @Test
  @DisplayName("Только один ребёнок — не симметрия")
  void singleChild() {
    assertThat(newSolution().isSymmetric(TreeNode.of(1, 2))).isFalse();
    assertThat(newSolution().isSymmetric(TreeNode.of(1, null, 2))).isFalse();
  }

  @Test
  @DisplayName("Глубокая симметрия с разными значениями")
  void deeperAsymmetry() {
    assertThat(newSolution().isSymmetric(TreeNode.of(2, 3, 3, 4, 5, 6, 4))).isFalse();
  }
}
