package dev.kata.warmup.week3;

/**
 * Warmup 3.6 — Isomorphic Strings (LeetCode #205).
 *
 * <h2>Условие</h2>
 *
 * <p>Строки изоморфны, если можно заменить каждый символ первой строки некоторым символом второй
 * так, чтобы получилась вторая строка. Замена должна быть взаимно однозначной: разные символы
 * первой строки не могут отображаться в один и тот же символ второй, и наоборот.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * isIsomorphic("egg", "add")     → true
 * isIsomorphic("foo", "bar")     → false
 * isIsomorphic("paper", "title") → true
 * isIsomorphic("badc", "baba")   → false
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code s.length}, {@code t.length} ≤ 5 × 10⁴;
 *   <li>строки состоят из печатных ASCII-символов.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1): два массива последних позиций размером 256. На каждом шаге позиция
 * символа {@code s} должна совпадать с позицией символа {@code t}; затем обе позиции обновляются
 * одинаковым значением {@code i + 1}. Одного массива недостаточно — он не различит отображение
 * «многие в один».
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week3.Warmup3_06_IsomorphicStringsSolution}.
 */
public class Warmup3_06_IsomorphicStrings {

  /** Проверяет взаимно однозначное соответствие символов двух строк. */
  public boolean isIsomorphic(String s, String t) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
