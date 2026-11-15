package dev.kata.solutions.easy;

import dev.kata.easy.Easy12_FlatMapDistinctSorted;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Эталон Easy 12.
 *
 * <p>Сначала множество нормализованных названий существующих курсов. Затем все пререквизиты
 * разворачиваются {@code flatMap}, нормализуются тем же способом, отбрасываются пустые, и {@code
 * distinct} убирает повторы до проверки на существование. Остаток сортируется компаратором {@code
 * comparingInt(length).thenComparing(naturalOrder())}.
 *
 * <p>{@code Locale.ROOT} делает lower-case независимым от локали. {@code Set} существующих курсов
 * даёт O(1) проверку; {@code toList()} — неизменяемый результат.
 */
public final class Easy12_FlatMapDistinctSortedSolution extends Easy12_FlatMapDistinctSorted {

  @Override
  public List<String> missingPrerequisites(List<Course> courses) {
    Objects.requireNonNull(courses, "courses");

    Set<String> existingCourses =
        courses.stream()
            .map(Course::title)
            .map(Easy12_FlatMapDistinctSortedSolution::normalize)
            .collect(Collectors.toSet());

    return courses.stream()
        .flatMap(course -> course.prerequisites().stream())
        .filter(Objects::nonNull)
        .map(Easy12_FlatMapDistinctSortedSolution::normalize)
        .filter(prerequisite -> !prerequisite.isEmpty())
        .distinct()
        .filter(prerequisite -> !existingCourses.contains(prerequisite))
        .sorted(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()))
        .toList();
  }

  private static String normalize(String value) {
    return value.strip().toLowerCase(Locale.ROOT);
  }
}
