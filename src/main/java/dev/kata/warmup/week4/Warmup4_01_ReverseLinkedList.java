package dev.kata.warmup.week4;

import dev.kata.warmup.common.ListNode;

/**
 * Warmup 4.1 — Reverse Linked List (LeetCode #206).
 *
 * <h2>Условие</h2>
 *
 * <p>Развернуть односвязный список и вернуть новую голову. Пустой список и список из одного узла
 * остаются пустым списком и тем же узлом. Менять значения узлов нельзя — нужно переставлять ссылки
 * {@code next}.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * reverseList([1,2,3,4,5]) → [5,4,3,2,1]
 * reverseList([1,2])       → [2,1]
 * reverseList([])          → []
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ длина списка ≤ 5000;
 *   <li>-5000 ≤ {@code node.val} ≤ 5000.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1) — итеративный разворот тремя указателями {@code
 * previous/current/next}. Рекурсивный вариант тоже O(n) по времени, но O(n) стека и риск {@link
 * StackOverflowError} на длинном списке.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week4.Warmup4_01_ReverseLinkedListSolution}.
 */
public class Warmup4_01_ReverseLinkedList {

  /** Разворачивает список и возвращает новую голову. */
  public ListNode reverseList(ListNode head) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
