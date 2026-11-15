package dev.kata.warmup.week4;

import dev.kata.warmup.common.ListNode;

/**
 * Warmup 4.3 — Linked List Cycle (LeetCode #141).
 *
 * <h2>Условие</h2>
 *
 * <p>Определить, содержит ли односвязный список цикл. Цикл задаётся тем, что {@code next}
 * некоторого узла указывает на один из предыдущих узлов. Изменять список нельзя, дополнительную
 * память использовать нельзя — нужен алгоритм Флойда (черепаха и заяц).
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * [3,2,0,-4], хвост указывает на индекс 1 → true
 * [1,2], хвост указывает на себя          → true
 * [1], без цикла                          → false
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ длина списка ≤ 10⁴;
 *   <li>-10⁵ ≤ {@code node.val} ≤ 10⁵;
 *   <li>{@code pos} — индекс, на который указывает хвост, или -1.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1): медленный указатель делает один шаг, быстрый — два. Если они
 * встретились, цикл есть; если быстрый дошёл до {@code null}, цикла нет. {@code HashSet} узлов был
 * бы O(n) по памяти.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week4.Warmup4_03_LinkedListCycleSolution}.
 */
public class Warmup4_03_LinkedListCycle {

  /** Проверяет наличие цикла. */
  public boolean hasCycle(ListNode head) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
