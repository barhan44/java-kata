package dev.kata.warmup.week4;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.solutions.warmup.week4.Warmup4_03_LinkedListCycleSolution;
import dev.kata.warmup.common.ListNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Warmup 4.3: Linked List Cycle")
class Warmup4_03_LinkedListCycleTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Warmup4_03_LinkedListCycle newSolution() {
    return USE_SOLUTION
        ? new Warmup4_03_LinkedListCycleSolution()
        : new Warmup4_03_LinkedListCycle();
  }

  @Test
  @DisplayName("Цикл в середине списка")
  void cycleInMiddle() {
    assertThat(newSolution().hasCycle(withCycle(new int[] {3, 2, 0, -4}, 1))).isTrue();
  }

  @Test
  @DisplayName("Цикл начинается с головы")
  void cycleAtHead() {
    assertThat(newSolution().hasCycle(withCycle(new int[] {3, 2, 0, -4}, 0))).isTrue();
  }

  @Test
  @DisplayName("Цикл в последнем узле")
  void cycleAtTail() {
    assertThat(newSolution().hasCycle(withCycle(new int[] {1, 2}, 1))).isTrue();
  }

  @Test
  @DisplayName("Список без цикла")
  void noCycle() {
    assertThat(newSolution().hasCycle(ListNode.of(1, 2, 3, 4))).isFalse();
    assertThat(newSolution().hasCycle(ListNode.of(1))).isFalse();
    assertThat(newSolution().hasCycle(null)).isFalse();
  }

  @Test
  @DisplayName("Один узел, ссылающийся сам на себя")
  void selfCycle() {
    ListNode single = new ListNode(1);
    single.next = single;
    assertThat(newSolution().hasCycle(single)).isTrue();
  }

  @Test
  @DisplayName("Длинный список с циклом")
  void longCycle() {
    assertThat(newSolution().hasCycle(withCycle(new int[] {1, 2, 3, 4, 5, 6, 7, 8}, 3))).isTrue();
  }

  /** Строит реальный список: хвост ссылается на узел с индексом {@code cyclePosition}. */
  private static ListNode withCycle(int[] values, int cyclePosition) {
    ListNode head = ListNode.of(values);
    ListNode tail = head;
    while (tail.next != null) {
      tail = tail.next;
    }
    ListNode cycleEntry = head;
    for (int i = 0; i < cyclePosition; i++) {
      cycleEntry = cycleEntry.next;
    }
    tail.next = cycleEntry;
    return head;
  }
}
