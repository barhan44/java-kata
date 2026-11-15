package dev.kata.solutions.warmup.week4;

import dev.kata.warmup.common.ListNode;
import dev.kata.warmup.week4.Warmup4_01_ReverseLinkedList;

/**
 * Эталон Warmup 4.1.
 *
 * <p>Итеративный разворот: на каждом шаге запоминаем {@code next}, переставляем {@code
 * current.next} на {@code previous} и сдвигаем оба указателя. Память O(1), глубина стека не зависит
 * от длины списка.
 *
 * <p>Рекурсивный эквивалент (для сравнения, в эталоне не используется):
 *
 * <pre>{@code
 * if (head == null || head.next == null) return head;
 * ListNode reversed = reverseList(head.next);
 * head.next.next = head;
 * head.next = null;
 * return reversed;
 * }</pre>
 *
 * <p>Он элегантен, но на 5000 узлах занимает 5000 стековых кадров; итеративная версия безопаснее.
 */
public final class Warmup4_01_ReverseLinkedListSolution extends Warmup4_01_ReverseLinkedList {

  @Override
  public ListNode reverseList(ListNode head) {
    ListNode previous = null;
    ListNode current = head;
    while (current != null) {
      ListNode next = current.next;
      current.next = previous;
      previous = current;
      current = next;
    }
    return previous;
  }
}
