package dev.kata.warmup.week3;

/**
 * Warmup 3.4 — Roman to Integer (LeetCode #13).
 *
 * <h2>Условие</h2>
 *
 * <p>Преобразовать римское число в целое. Символы: I=1, V=5, X=10, L=50, C=100, D=500, M=1000. Если
 * меньший символ стоит перед большим, он вычитается (IV=4, IX=9, XL=40, XC=90, CD=400, CM=900).
 * Использовать современный {@code switch} expression.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * romanToInt("III")     → 3
 * romanToInt("LVIII")   → 58
 * romanToInt("MCMXCIV") → 1994
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code s.length()} ≤ 15;
 *   <li>строка содержит только символы I, V, X, L, C, D, M и является корректным римским числом.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1). Идём справа налево и сравниваем текущий символ со следующим: если
 * текущий меньше — вычитаем, иначе прибавляем. Это избавляет от поиска конкретных комбинаций вида
 * IV/IX.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week3.Warmup3_04_RomanToIntegerSolution}.
 */
public class Warmup3_04_RomanToInteger {

  /** Преобразует корректную римскую запись в число. */
  public int romanToInt(String s) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
