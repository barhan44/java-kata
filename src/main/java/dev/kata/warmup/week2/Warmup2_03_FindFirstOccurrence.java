package dev.kata.warmup.week2;

/**
 * Warmup 2.3 — Find the Index of the First Occurrence (LeetCode #28).
 *
 * <h2>Условие</h2>
 *
 * <p>Вернуть индекс первого вхождения {@code needle} в {@code haystack}, либо {@code -1}. Пустая
 * игла считается вхождением с индексом 0. Пользоваться {@code String.indexOf}, {@code contains},
 * {@code split} и регулярными выражениями нельзя.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * indexOf("sadbutsad", "sad") → 0
 * indexOf("leetcode", "leeto") → -1
 * indexOf("mississippi", "issip") → 4
 * indexOf("abc", "") → 0
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code haystack.length}, {@code needle.length} ≤ 10⁴;
 *   <li>строки состоят из строчных латинских букв.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n + m), память O(m) — алгоритм Кнута-Морриса-Пратта. Наивное сравнение даёт O(n × m)
 * на строках вида {@code "aaaa...a"} и {@code "aaaab"}; LPS-массив позволяет не откатывать
 * указатель {@code haystack} назад.
 *
 * <p>Реализацию см. в {@code
 * dev.kata.solutions.warmup.week2.Warmup2_03_FindFirstOccurrenceSolution}.
 */
public class Warmup2_03_FindFirstOccurrence {

  /**
   * Ищет первое вхождение {@code needle} в {@code haystack}.
   *
   * @return индекс или -1
   */
  public int indexOf(String haystack, String needle) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
