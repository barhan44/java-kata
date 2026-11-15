package dev.kata.solutions.easy;

import dev.kata.easy.Easy33_RecursiveMenuFlattener;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Эталон Easy 33.
 *
 * <p>{@code flatten(item, parentPath)} возвращает поток из пары {@code code → path} и рекурсивно
 * склеенного потока детей. {@code Stream.concat} сохраняет pre-order, а {@code flatMap} на уровне
 * корней разворачивает дерево без ручного стека. {@code toMap(..., LinkedHashMap::new)} фиксирует
 * порядок обхода; merge-функция оставляет последнее значение при повторном code, не меняя позицию.
 *
 * <p>Пустые {@code children} дают пустой поток, поэтому рекурсия естественно останавливается.
 */
public final class Easy33_RecursiveMenuFlattenerSolution extends Easy33_RecursiveMenuFlattener {

  private static final String SEPARATOR = " / ";

  @Override
  public Map<String, String> flattenMenuPaths(List<MenuItem> roots) {
    Objects.requireNonNull(roots, "roots");

    Map<String, String> flattened =
        roots.stream()
            .flatMap(root -> flatten(root, ""))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (left, right) -> right,
                    LinkedHashMap::new));
    return Collections.unmodifiableMap(flattened);
  }

  private static Stream<Map.Entry<String, String>> flatten(MenuItem item, String parentPath) {
    String path = parentPath.isEmpty() ? item.title() : parentPath + SEPARATOR + item.title();
    return Stream.concat(
        Stream.of(Map.entry(item.code(), path)),
        item.children().stream().flatMap(child -> flatten(child, path)));
  }
}
