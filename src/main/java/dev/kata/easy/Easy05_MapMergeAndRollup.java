package dev.kata.easy;

import java.util.Map;

/**
 * Easy 05. Слияние балансов через Map.merge.
 *
 * <h2>Задача</h2>
 *
 * <p>Даны две карты «ключ → баланс»: базовая и дельта изменений. Сложить значения по совпадающим
 * ключам и вернуть только ключи с положительным итогом (все итоги {@code <= 0} исключаются).
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * base  = {"a": 10, "b": 5, "c": 3}
 * delta = {"a": -10, "b": 2, "d": 7, "e": -1}
 *
 * mergeBalances → {"b": 7, "c": 3, "d": 7}
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>ключи и значения карт не {@code null};
 *   <li>результат не зависит от порядка обхода; исходные карты не изменяются.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(N + M), память O(N + M). Эталон начинает с копии {@code base}, применяет {@code
 * Map.merge(key, value, Integer::sum)} для каждой дельты и удаляет неположительные итоги через
 * {@code entrySet().removeIf}. Возвращаемая карта неизменяема.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy05_MapMergeAndRollupSolution}.
 */
public class Easy05_MapMergeAndRollup {

  /** Суммирует балансы и отбрасывает итоги ≤ 0. */
  public Map<String, Integer> mergeBalances(Map<String, Integer> base, Map<String, Integer> delta) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
