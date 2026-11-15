package dev.kata.solutions.warmup.week4;

import dev.kata.warmup.common.ListNode;
import dev.kata.warmup.week4.Warmup4_03_LinkedListCycle;

/**
 * Эталон Warmup 4.3.
 *
 * <p>Флойд: {@code slow} и {@code fast} стартуют с головы. На каждой итерации {@code slow} делает
 * один шаг, {@code fast} — два. В конечном списке быстрый рано или поздно встретит {@code null}; в
 * списке с циклом он зайдёт в цикл и догонит медленного. Проверка {@code fast.next != null} перед
 * вторым шагом защищает от NPE на последнем узле.
 */
public final class Warmup4_03_LinkedListCycleSolution extends Warmup4_03_LinkedListCycle {

  @Override
  public boolean hasCycle(ListNode head) {
    ListNode slow = head;
    ListNode fast = head;

    while (fast != null && fast.next != null) {
      slow = slow.next;
      fast = fast.next.next;
      if (slow == fast) {
        return true;
      }
    }
    return false;
  }
}
