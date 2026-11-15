package dev.kata.warmup.week4;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week4.Warmup4_02_MergeTwoSortedListsSolution;
import dev.kata.warmup.common.ListNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 4.2: Merge Two Sorted Lists")
class Warmup4_02_MergeTwoSortedListsTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup4_02_MergeTwoSortedLists newSolution() {
    return USE_SOLUTION
        ? new Warmup4_02_MergeTwoSortedListsSolution()
        : new Warmup4_02_MergeTwoSortedLists();
  }

  @Test
  @DisplayName("Классический пример с дубликатами")
  void mergesLists() {
    ListNode merged = newSolution().mergeTwoLists(ListNode.of(1, 2, 4), ListNode.of(1, 3, 4));
    assertThat(merged.toList()).containsExactly(1, 1, 2, 3, 4, 4);
  }

  @Test
  @DisplayName("Оба списка пусты")
  void bothEmpty() {
    assertThat(newSolution().mergeTwoLists(null, null)).isNull();
  }

  @Test
  @DisplayName("Один список пуст")
  void oneEmpty() {
    assertThat(newSolution().mergeTwoLists(null, ListNode.of(0)).toList()).containsExactly(0);
    assertThat(newSolution().mergeTwoLists(ListNode.of(1, 2), null).toList()).containsExactly(1, 2);
  }

  @Test
  @DisplayName("Отрицательные значения и один список исчерпан раньше")
  void negativesAndExhaustion() {
    assertThat(newSolution().mergeTwoLists(ListNode.of(-5, -1, 4), ListNode.of(-3, 0, 9)).toList())
        .containsExactly(-5, -3, -1, 0, 4, 9);
    assertThat(newSolution().mergeTwoLists(ListNode.of(1, 2, 3), ListNode.of(4, 5)).toList())
        .containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  @DisplayName("Узлы переиспользуются, исходные головы не ломаются")
  void reusesNodes() {
    ListNode first = ListNode.of(1, 3);
    ListNode second = ListNode.of(2, 4);
    ListNode firstTail = first.next;
    ListNode secondTail = second.next;

    ListNode merged = newSolution().mergeTwoLists(first, second);
    assertThat(merged.toList()).containsExactly(1, 2, 3, 4);
    assertThat(merged).isSameAs(first);
    assertThat(merged.next).isSameAs(second);
    assertThat(merged.next.next).isSameAs(firstTail);
    assertThat(merged.next.next.next).isSameAs(secondTail);
  }
}
