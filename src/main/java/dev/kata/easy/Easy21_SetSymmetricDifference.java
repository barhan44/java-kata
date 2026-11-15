package dev.kata.easy;

import java.util.Set;

/**
 * Easy 21. Алгебра множеств: симметрическая разность и подмножество.
 *
 * <h2>Задача</h2>
 *
 * <ul>
 *   <li>{@code symmetricDifference(a, b)} — элементы, входящие ровно в одно из множеств;
 *   <li>{@code isSubset(candidate, superset)} — все элементы {@code candidate} есть в {@code
 *       superset}.
 * </ul>
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * symmetricDifference({1,2,3}, {3,4}) → {1,2,4}
 * isSubset({1,2}, {1,2,3})            → true
 * isSubset({1,4}, {1,2,3})            → false
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>множества не {@code null}; элементы могут быть любого типа.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(|a| + |b|), память O(|result|). Эталон не создаёт копий входов: фильтрует каждое
 * множество по принадлежности другому. Результат — неизменяемый {@code LinkedHashSet} в порядке
 * «сначала только из a, затем только из b».
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy21_SetSymmetricDifferenceSolution}.
 */
public class Easy21_SetSymmetricDifference {

  /** Возвращает симметрическую разность двух множеств. */
  public <T> Set<T> symmetricDifference(Set<T> a, Set<T> b) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /** Проверяет, является ли {@code candidate} подмножеством {@code superset}. */
  public <T> boolean isSubset(Set<T> candidate, Set<T> superset) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
