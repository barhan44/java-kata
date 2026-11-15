package dev.kata.solutions.easy;

import dev.kata.easy.Easy14_NestedGroupingRollup;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Эталон Easy 14.
 *
 * <p>Два уровня {@code groupingBy} с {@link TreeMap} дают детерминированный порядок регионов и
 * категорий. Внутри категории {@code toMap(product, revenue, Integer::sum)} суммирует доход по
 * продукту, а {@code collectingAndThen} выбирает максимум по сумме. Компаратор {@code
 * comparingByValue().thenComparing(comparingByKey(reverseOrder()))} при равной сумме оставляет
 * меньший product.
 *
 * <p>Вложенные карты собираются заново в unmodifiable-обёртки: нельзя изменить ни региональную
 * карту, ни карту категорий.
 */
public final class Easy14_NestedGroupingRollupSolution extends Easy14_NestedGroupingRollup {

  @Override
  public Map<String, Map<String, String>> bestProductByRegionAndCategory(List<Sale> sales) {
    Objects.requireNonNull(sales, "sales");

    Map<String, Map<String, String>> grouped =
        sales.stream()
            .collect(
                Collectors.groupingBy(
                    Sale::region,
                    TreeMap::new,
                    Collectors.groupingBy(
                        Sale::category,
                        TreeMap::new,
                        Collectors.collectingAndThen(
                            Collectors.toMap(Sale::product, Sale::revenue, Integer::sum),
                            byProduct ->
                                byProduct.entrySet().stream()
                                    .max(
                                        Map.Entry.<String, Integer>comparingByValue()
                                            .thenComparing(
                                                Map.Entry.comparingByKey(
                                                    Comparator.reverseOrder())))
                                    .map(Map.Entry::getKey)
                                    .orElseThrow()))));

    Map<String, Map<String, String>> result = new TreeMap<>();
    grouped.forEach(
        (region, categories) ->
            result.put(region, Collections.unmodifiableMap(new TreeMap<>(categories))));
    return Collections.unmodifiableMap(result);
  }
}
