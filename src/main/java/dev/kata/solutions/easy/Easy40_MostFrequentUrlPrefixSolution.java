package dev.kata.solutions.easy;

import dev.kata.easy.Easy40_MostFrequentUrlPrefix;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Эталон Easy 40.
 *
 * <p>{@code prefixOf} отбрасывает query/fragment, режет путь по {@code "/"}, игнорирует пустые
 * сегменты, оставляет первые {@code depth} и склеивает их с ведущим слешем. {@code depth == 0} или
 * пустой путь дают {@code "/"}.
 *
 * <p>Частоты считаются {@code groupingBy(identity, counting())}; сортировка — «частота по убыванию,
 * ключ по возрастанию», затем {@code limit(topK)}. Пустые пути и дубликаты считаются как обычные
 * вхождения.
 */
public final class Easy40_MostFrequentUrlPrefixSolution extends Easy40_MostFrequentUrlPrefix {

  @Override
  public List<String> topPrefixSegments(List<String> paths, int depth, int topK) {
    Objects.requireNonNull(paths, "paths");
    if (depth < 0) {
      throw new IllegalArgumentException("depth must be >= 0, got " + depth);
    }
    if (topK < 0) {
      throw new IllegalArgumentException("topK must be >= 0, got " + topK);
    }
    if (topK == 0) {
      return List.of();
    }

    Map<String, Long> frequencies =
        paths.stream()
            .map(path -> prefixOf(path, depth))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    return frequencies.entrySet().stream()
        .sorted(
            Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
        .limit(topK)
        .map(Map.Entry::getKey)
        .toList();
  }

  private static String prefixOf(String path, int depth) {
    Objects.requireNonNull(path, "path");

    String clean = path.strip();
    int query = clean.indexOf('?');
    if (query >= 0) {
      clean = clean.substring(0, query);
    }
    int fragment = clean.indexOf('#');
    if (fragment >= 0) {
      clean = clean.substring(0, fragment);
    }

    List<String> segments =
        Arrays.stream(clean.split("/")).filter(segment -> !segment.isEmpty()).limit(depth).toList();
    return segments.isEmpty() ? "/" : "/" + String.join("/", segments);
  }
}
