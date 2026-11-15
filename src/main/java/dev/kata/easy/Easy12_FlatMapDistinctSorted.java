package dev.kata.easy;

import java.util.List;
import java.util.Objects;

/**
 * Easy 12. FlatMap + distinct + sorted.
 *
 * <h2>Задача</h2>
 *
 * <p>Дан список курсов с пререквизитами. Собрать уникальные пререквизиты, нормализовать их (trim +
 * lowerCase), исключить те, которые уже являются курсами из списка, и отсортировать результат по
 * длине, затем по алфавиту.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * courses = [
 *   Course("Java", [" OOP ", "Algorithms"]),
 *   Course("OOP",  ["Programming Basics"])
 * ]
 *
 * missingPrerequisites → ["algorithms", "programming basics"]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code courses.size()};
 *   <li>названия курсов не {@code null}/не blank; список пререквизитов не {@code null};
 *   <li>blank-пререквизиты игнорируются.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(T log U), память O(U), где T — число пререквизитов, U — число уникальных. Эталон —
 * цепочка {@code flatMap → map → filter → distinct → filter → sorted}; дубликаты исчезают до
 * сортировки. Компаратор: длина по возрастанию, затем естественный порядок строк. Результат
 * неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy12_FlatMapDistinctSortedSolution}.
 */
public class Easy12_FlatMapDistinctSorted {

  /** Курс и его пререквизиты. */
  public record Course(String title, List<String> prerequisites) {
    public Course {
      Objects.requireNonNull(title, "title");
      if (title.isBlank()) {
        throw new IllegalArgumentException("title must not be blank");
      }
      prerequisites = List.copyOf(Objects.requireNonNull(prerequisites, "prerequisites"));
    }
  }

  /** Возвращает недостающие пререквизиты в порядке «длина, затем алфавит». */
  public List<String> missingPrerequisites(List<Course> courses) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
