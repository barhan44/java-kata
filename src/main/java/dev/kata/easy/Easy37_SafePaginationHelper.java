package dev.kata.easy;

import java.util.List;
import java.util.Objects;

/**
 * Easy 37. Безопасная пагинация в памяти.
 *
 * <h2>Задача</h2>
 *
 * <p>Разбить список на страницы с 1-based номером. Вернуть {@link Page} с элементами запрошенной
 * страницы, её номером, размером, общим числом элементов и страниц. Номер страницы за пределами
 * диапазона допустим: элементы пусты, но метаданные сохраняются.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * paginate([1,2,3,4,5], page=2, size=2)
 *   → Page(items=[3,4], pageNumber=2, pageSize=2, totalItems=5, totalPages=3)
 *
 * paginate([1,2,3,4,5], page=4, size=2)
 *   → Page(items=[],    pageNumber=4, pageSize=2, totalItems=5, totalPages=3)
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code allItems.size()};
 *   <li>{@code pageNumber ≥ 1}, {@code pageSize ≥ 1}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(pageSize), память O(pageSize) на копию страницы. {@code Math.ceilDiv} считает число
 * страниц без переполнения; смещение вычисляется в {@code long}, поэтому огромный номер страницы не
 * ломает арифметику. Возвращаемые списки неизменяемы.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy37_SafePaginationHelperSolution}.
 */
public class Easy37_SafePaginationHelper {

  /** Страница результата. */
  public record Page<T>(
      List<T> items, int pageNumber, int pageSize, int totalItems, int totalPages) {
    public Page {
      items = List.copyOf(Objects.requireNonNull(items, "items"));
      if (pageNumber < 1) {
        throw new IllegalArgumentException("pageNumber must be >= 1");
      }
      if (pageSize < 1) {
        throw new IllegalArgumentException("pageSize must be >= 1");
      }
      if (totalItems < 0 || totalPages < 0) {
        throw new IllegalArgumentException("totalItems/totalPages must be >= 0");
      }
    }
  }

  /** Возвращает запрошенную страницу; за пределами — пустую. */
  public <T> Page<T> paginate(List<T> allItems, int pageNumber, int pageSize) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
