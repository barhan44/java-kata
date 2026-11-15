package dev.kata.solutions.easy;

import dev.kata.easy.Easy37_SafePaginationHelper;
import java.util.List;
import java.util.Objects;

/**
 * Эталон Easy 37.
 *
 * <p>{@code totalPages = Math.ceilDiv(totalItems, pageSize)}: для пустого списка это 0, для 5
 * элементов по 2 — 3. Смещение считается как {@code (long)(pageNumber - 1) * pageSize}; если оно
 * уже за пределами списка, возвращается пустая страница без {@code subList} и исключений. Иначе
 * границы приводятся к {@code int} и копируются через {@code List.copyOf}.
 *
 * <p>Метаданные всегда отражают исходный список, даже если запрошена несуществующая страница.
 */
public final class Easy37_SafePaginationHelperSolution extends Easy37_SafePaginationHelper {

  @Override
  public <T> Page<T> paginate(List<T> allItems, int pageNumber, int pageSize) {
    Objects.requireNonNull(allItems, "allItems");
    if (pageNumber < 1) {
      throw new IllegalArgumentException("pageNumber must be >= 1, got " + pageNumber);
    }
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be >= 1, got " + pageSize);
    }

    int totalItems = allItems.size();
    int totalPages = Math.ceilDiv(totalItems, pageSize);
    long offsetLong = (long) (pageNumber - 1) * pageSize;

    List<T> items;
    if (offsetLong >= totalItems) {
      items = List.of();
    } else {
      int from = (int) offsetLong;
      int to = (int) Math.min((long) from + pageSize, totalItems);
      items = List.copyOf(allItems.subList(from, to));
    }
    return new Page<>(items, pageNumber, pageSize, totalItems, totalPages);
  }
}
