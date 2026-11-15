package dev.kata.easy;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Easy 35. Фильтрация и проекция табличных строк.
 *
 * <h2>Задача</h2>
 *
 * <p>Даны строки таблицы (каждая — карта колонок). Оставить строки, удовлетворяющие всем
 * предикатам, и спроецировать их на выбранные колонки. Порядок выбранных колонок сохраняется через
 * {@link java.util.LinkedHashMap}. Если в строке нет выбранной колонки, в проекции остаётся ключ со
 * значением {@code null}; если нет колонки фильтра, предикат получает {@code null}.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * rows = [
 *   {"name":"Alice", "dept":"Eng", "salary":"120"},
 *   {"name":"Bob",   "dept":"Eng", "salary":"80"},
 *   {"name":"Carol", "dept":"Sales","salary":"90"}
 * ]
 * filters = {"dept": "Eng"::equals, "salary": s -> Integer.parseInt(s) > 100}
 * select  = ["name", "salary"]
 *
 * → [{"name":"Alice", "salary":"120"}]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ размеры {@code rows}, {@code filters}, {@code selectColumns};
 *   <li>имена колонок и предикаты не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(R × (F + C)), память O(R × C) на результат. Эталон фильтрует строки через {@code
 * allMatch} по предикатам, затем собирает проекцию в {@code LinkedHashMap}. Входные строки и их
 * карты не изменяются; результат и каждая строка неизменяемы.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy35_CsvQueryFilterSolution}.
 */
public class Easy35_CsvQueryFilter {

  /** Фильтрует строки и оставляет только выбранные колонки в заданном порядке. */
  public List<Map<String, String>> filterAndProject(
      List<Map<String, String>> rows,
      Map<String, Predicate<String>> filters,
      List<String> selectColumns) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
