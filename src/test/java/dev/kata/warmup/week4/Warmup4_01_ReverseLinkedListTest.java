package dev.kata.warmup.week4;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week4.Warmup4_01_ReverseLinkedListSolution;
import dev.kata.warmup.common.ListNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 4.1: Reverse Linked List")
class Warmup4_01_ReverseLinkedListTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup4_01_ReverseLinkedList newSolution() {
    return USE_SOLUTION
        ? new Warmup4_01_ReverseLinkedListSolution()
        : new Warmup4_01_ReverseLinkedList();
  }

  @Test
  @DisplayName("Классический пример")
  void reversesList() {
    ListNode reversed = newSolution().reverseList(ListNode.of(1, 2, 3, 4, 5));
    assertThat(reversed.toList()).containsExactly(5, 4, 3, 2, 1);
  }

  @Test
  @DisplayName("Пустой список и один узел")
  void emptyAndSingle() {
    assertThat(newSolution().reverseList(null)).isNull();

    ListNode single = newSolution().reverseList(ListNode.of(7));
    assertThat(single.toList()).containsExactly(7);
  }

  @Test
  @DisplayName("Два узла и отрицательные значения")
  void twoNodesAndNegatives() {
    assertThat(newSolution().reverseList(ListNode.of(1, 2)).toList()).containsExactly(2, 1);
    assertThat(newSolution().reverseList(ListNode.of(-1, 0, 3)).toList()).containsExactly(3, 0, -1);
  }

  @Test
  @DisplayName("Длинный список разворачивается без рекурсии")
  void longList() {
    int size = 10_000;
    int[] values = new int[size];
    for (int i = 0; i < size; i++) {
      values[i] = i;
    }

    ListNode reversed = newSolution().reverseList(ListNode.of(values));

    assertThat(reversed.val).isEqualTo(size - 1);
    ListNode tail = reversed;
    int count = 1;
    while (tail.next != null) {
      tail = tail.next;
      count++;
    }
    assertThat(count).isEqualTo(size);
    assertThat(tail.val).isZero();
  }
}
