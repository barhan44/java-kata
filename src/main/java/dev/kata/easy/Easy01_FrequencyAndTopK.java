package dev.kata.easy;

import java.util.List;

/**
 * Easy 01. Частоты слов и топ-K (JCF + Stream API).
 *
 * <h2>Задача</h2>
 *
 * <p>Дан список слов. Привести слова к нижнему регистру, обрезать пробелы, игнорировать {@code
 * null}, пустые и blank-строки, посчитать частоты и вернуть {@code k} самых частых слов. При равной
 * частоте слова идут по алфавиту.
 *
 * <h2>Примеры</h2>
 *
 * <pre>
 * topKFrequentWords(["the","day","is","sunny","the","the","the","sunny","is","is"], 4)
 *   → ["the","is","sunny","day"]
 *
 * topKFrequentWords(["Apple"," apple ","BANANA","banana","  "], 2)
 *   → ["apple","banana"]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code words.size()} и {@code k};
 *   <li>сравнение регистронезависимое; порядок при равной частоте — лексикографический.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n + u log u), память O(u), где u — число уникальных слов. Эталон использует {@code
 * Collectors.groupingBy(..., counting())}, затем сортирует записи компаратором «частота по
 * убыванию, ключ по возрастанию». Возвращаемый список неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy01_FrequencyAndTopKSolution}.
 */
public class Easy01_FrequencyAndTopK {

  /**
   * Возвращает топ-K слов по частоте.
   *
   * @param words исходные слова; {@code null}-элементы игнорируются
   * @param k желаемое число слов
   * @return неизменяемый список слов
   * @throws IllegalArgumentException если {@code k < 0}
   */
  public List<String> topKFrequentWords(List<String> words, int k) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
