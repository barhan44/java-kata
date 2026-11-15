package dev.kata.warmup.week2;

/**
 * Warmup 2.7 — Valid Anagram (LeetCode #242).
 *
 * <h2>Условие</h2>
 *
 * <p>Проверить, является ли {@code t} анаграммой {@code s}: строки состоят из одних и тех же букв с
 * одинаковыми кратностями, порядок не важен. Использовать массив частот {@code int[26]}, а не
 * сортировку.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * isAnagram("anagram", "nagaram") → true
 * isAnagram("rat", "car")         → false
 * isAnagram("a", "ab")            → false
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code s.length}, {@code t.length} ≤ 5 × 10⁴;
 *   <li>строки состоят из строчных латинских букв {@code 'a'..'z'}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1) — 26 счётчиков. Сначала сравниваем длины, затем одним проходом
 * увеличиваем счётчики для {@code s} и уменьшаем для {@code t}. Все нули означают анаграмму.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week2.Warmup2_07_ValidAnagramSolution}.
 */
public class Warmup2_07_ValidAnagram {

  /** Проверяет, являются ли строки анаграммами. */
  public boolean isAnagram(String s, String t) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
