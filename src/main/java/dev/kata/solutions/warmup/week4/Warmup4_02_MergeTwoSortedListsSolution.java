package dev.kata.solutions.warmup.week4;

import dev.kata.warmup.common.ListNode;
import dev.kata.warmup.week4.Warmup4_02_MergeTwoSortedLists;

/**
 * Эталон Warmup 4.2.
 *
 * <p>Dummy-узел служит якорем результата: {@code tail} всегда указывает на последний добавленный
 * узел, а возвращается {@code dummy.next}. Сравниваем головы и присоединяем меньшую; при равенстве
 * порядок между списками не важен. Когда один список закончился, второй присоединяется одним
 * указателем — копировать его узлы не нужно.
 */
public final class Warmup4_02_MergeTwoSortedListsSolution extends Warmup4_02_MergeTwoSortedLists {

  @Override
  public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
    ListNode dummy = new ListNode(0);
    ListNode tail = dummy;
    ListNode first = list1;
    ListNode second = list2;

    while (first != null && second != null) {
      if (first.val <= second.val) {
        tail.next = first;
        first = first.next;
      } else {
        tail.next = second;
        second = second.next;
      }
      tail = tail.next;
    }

    tail.next = first != null ? first : second;
    return dummy.next;
  }
}
