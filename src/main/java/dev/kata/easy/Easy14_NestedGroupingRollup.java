package dev.kata.easy;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Easy 14. Вложенная группировка и rollup.
 *
 * <h2>Задача</h2>
 *
 * <p>Даны продажи. Построить вложенную карту {@code region → category → product}, где продукт —
 * самый доходный в категории. Доход продукта суммируется по всем его продажам. При равенстве
 * доходов выбирается продукт, меньший по алфавиту.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * sales = [
 *   Sale("North", "Toys", "Lego",   100),
 *   Sale("North", "Toys", "Barbie", 150),
 *   Sale("North", "Toys", "Lego",   100)
 * ]
 *
 * result → {"North": {"Toys": "Lego"}}   // Lego: 200, Barbie: 150
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code sales.size()};
 *   <li>{@code region}, {@code category}, {@code product} не {@code null}/не blank.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n + G log G), память O(n + G), где G — число групп и товаров. Эталон — вложенный
 * {@code groupingBy(region, TreeMap, groupingBy(category, TreeMap, collectingAndThen(toMap(product,
 * revenue, sum), max)))}. Суммирование по продукту важно: продукт с несколькими мелкими продажами
 * может обогнать одну крупную. Карты неизменяемы.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy14_NestedGroupingRollupSolution}.
 */
public class Easy14_NestedGroupingRollup {

  /** Продажа. */
  public record Sale(String region, String category, String product, int revenue) {
    public Sale {
      requireText(region, "region");
      requireText(category, "category");
      requireText(product, "product");
    }
  }

  /** Возвращает region → category → самый доходный product. */
  public Map<String, Map<String, String>> bestProductByRegionAndCategory(List<Sale> sales) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
