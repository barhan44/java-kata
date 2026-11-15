package dev.kata.solutions.easy;

import dev.kata.easy.Easy35_CsvQueryFilter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Эталон Easy 35.
 *
 * <p>Фильтр — это {@code filters.entrySet().stream().allMatch(...)}: каждое условие применяется к
 * значению колонки текущей строки, отсутствующая колонка даёт {@code null}. Пустая карта фильтров
 * пропускает все строки. Проекция строится в порядке {@code selectColumns} через {@code
 * LinkedHashMap}; отсутствующая колонка сохраняется как {@code null}, чтобы форма результата не
 * зависела от данных.
 *
 * <p>{@code Collections.unmodifiableMap} защищает каждую строку, {@code Stream.toList()} — весь
 * результат. Исходные карты не сортируются и не меняются.
 */
public final class Easy35_CsvQueryFilterSolution extends Easy35_CsvQueryFilter {

  @Override
  public List<Map<String, String>> filterAndProject(
      List<Map<String, String>> rows,
      Map<String, Predicate<String>> filters,
      List<String> selectColumns) {
    Objects.requireNonNull(rows, "rows");
    Objects.requireNonNull(filters, "filters");
    Objects.requireNonNull(selectColumns, "selectColumns");

    return rows.stream()
        .filter(
            row ->
                filters.entrySet().stream()
                    .allMatch(
                        filter ->
                            Objects.requireNonNull(filter.getValue(), "filter")
                                .test(row.get(filter.getKey()))))
        .map(row -> project(row, selectColumns))
        .toList();
  }

  private static Map<String, String> project(Map<String, String> row, List<String> selectColumns) {
    Map<String, String> projected = new LinkedHashMap<>();
    selectColumns.forEach(
        column -> projected.put(Objects.requireNonNull(column, "column"), row.get(column)));
    return Collections.unmodifiableMap(projected);
  }
}
