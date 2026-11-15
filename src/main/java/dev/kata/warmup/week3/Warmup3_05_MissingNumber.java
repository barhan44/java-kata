package dev.kata.warmup.week3;

/**
 * Warmup 3.5 — Missing Number (LeetCode #268).
 *
 * <h2>Условие</h2>
 *
 * <p>Массив содержит {@code n} различных чисел из диапазона {@code [0, n]}. Ровно одно число
 * диапазона отсутствует — найти его за O(n) времени и O(1) памяти. Использовать XOR или формулу
 * Гаусса без переполнения.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * missingNumber([3,0,1]) → 2
 * missingNumber([0,1])   → 2
 * missingNumber([9,6,4,2,3,5,7,0,1]) → 8
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>1 ≤ {@code n} ≤ 10⁴;
 *   <li>0 ≤ {@code nums[i]} ≤ n;
 *   <li>все значения уникальны.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1). XOR всех индексов {@code 0..n} и всех значений: парные числа
 * уничтожаются, остаётся пропущенное. Формула Гаусса требует {@code long} или деления на 2 до
 * умножения, чтобы не переполнить {@code int} на больших n.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.warmup.week3.Warmup3_05_MissingNumberSolution}.
 */
public class Warmup3_05_MissingNumber {

  /** Возвращает единственное отсутствующее число. */
  public int missingNumber(int[] nums) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
