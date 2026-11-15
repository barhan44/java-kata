package dev.kata.warmup.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Узел односвязного списка в стиле LeetCode.
 *
 * <p>Поля {@code val}/{@code next} публичны, потому что задачи недели 4 меняют ссылки напрямую.
 * Фабрика {@link #of(int...)} избавляет тесты от ручного связывания узлов, а {@link #toList()} даёт
 * читаемое представление результата. Для списка с циклом {@code toList()} бросает {@link
 * IllegalStateException}, чтобы отладочный вывод не завис.
 */
public class ListNode {

  public int val;
  public ListNode next;

  public ListNode() {}

  public ListNode(int val) {
    this.val = val;
  }

  public ListNode(int val, ListNode next) {
    this.val = val;
    this.next = next;
  }

  /**
   * Строит список из значений; пустой массив даёт {@code null} (пустой список в LeetCode).
   *
   * @param values значения в порядке слева направо
   * @return голова списка или {@code null}
   */
  public static ListNode of(int... values) {
    Objects.requireNonNull(values, "values");
    ListNode dummy = new ListNode(0);
    ListNode tail = dummy;
    for (int value : values) {
      tail.next = new ListNode(value);
      tail = tail.next;
    }
    return dummy.next;
  }

  /**
   * Возвращает значения по порядку от текущего узла.
   *
   * @throws IllegalStateException если список содержит цикл
   */
  public List<Integer> toList() {
    List<Integer> values = new ArrayList<>();
    Set<ListNode> seen = new HashSet<>();
    ListNode current = this;
    while (current != null) {
      if (!seen.add(current)) {
        throw new IllegalStateException("Cycle detected in ListNode");
      }
      values.add(current.val);
      current = current.next;
    }
    return List.copyOf(values);
  }

  @Override
  public String toString() {
    return toList().toString();
  }
}
