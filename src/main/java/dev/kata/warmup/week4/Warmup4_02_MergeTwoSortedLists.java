package dev.kata.warmup.week4;

import dev.kata.warmup.common.ListNode;

/**
 * Warmup 4.2 — Merge Two Sorted Lists (LeetCode #21).
 *
 * <h2>Условие</h2>
 *
 * <p>Слить два отсортированных односвязных списка в один отсортированный, переиспользуя
 * существующие узлы. Вернуть голову результата. Пустые списки обрабатываются как обычно.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * mergeTwoLists([1,2,4], [1,3,4]) → [1,1,2,3,4,4]
 * mergeTwoLists([], [])          → []
 * mergeTwoLists([], [0])         → [0]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ длина каждого списка ≤ 50;
 *   <li>-100 ≤ {@code node.val} ≤ 100;
 *   <li>оба списка отсортированы по неубыванию.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n + m), память O(1) дополнительной (dummy-узел — одна аллокация). Dummy-узел избавляет
 * от отдельной обработки пустого результата и ветвления на первом элементе. Остаток непустого
 * списка присоединяется целиком.
 *
 * <p>Реализацию см. в {@code
 * dev.kata.solutions.warmup.week4.Warmup4_02_MergeTwoSortedListsSolution}.
 */
public class Warmup4_02_MergeTwoSortedLists {

  /** Сливает два отсортированных списка. */
  public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
