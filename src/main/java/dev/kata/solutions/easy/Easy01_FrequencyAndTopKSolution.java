package dev.kata.solutions.easy;

import dev.kata.easy.Easy01_FrequencyAndTopK;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Эталон Easy 01.
 *
 * <p>Конвейер из трёх шагов: нормализация → частоты → сортировка/лимит. {@code strip()} убирает
 * внешние пробелы до проверки на пустоту, {@code Locale.ROOT} исключает сюрпризы турецкой локали с
 * {@code I}. {@code counting()} возвращает {@code Long}, поэтому сортировка идёт по {@code Long}, а
 * не по {@code Integer}.
 *
 * <p>Компаратор записей читается сверху вниз: сначала частота по убыванию, затем ключ по
 * возрастанию. {@code limit(k)} не требует знать размер карты. {@code Stream.toList()} отдаёт
 * неизменяемый список.
 */
public final class Easy01_FrequencyAndTopKSolution extends Easy01_FrequencyAndTopK {

  @Override
  public List<String> topKFrequentWords(List<String> words, int k) {
    Objects.requireNonNull(words, "words");
    if (k < 0) {
      throw new IllegalArgumentException("k must be >= 0, got " + k);
    }
    if (k == 0) {
      return List.of();
    }

    Map<String, Long> frequencies =
        words.stream()
            .filter(Objects::nonNull)
            .map(String::strip)
            .filter(word -> !word.isEmpty())
            .map(word -> word.toLowerCase(Locale.ROOT))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    return frequencies.entrySet().stream()
        .sorted(
            Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
        .limit(k)
        .map(Map.Entry::getKey)
        .toList();
  }
}
