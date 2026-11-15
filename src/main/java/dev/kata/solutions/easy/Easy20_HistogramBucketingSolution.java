package dev.kata.solutions.easy;

import dev.kata.easy.Easy20_HistogramBucketing;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Эталон Easy 20.
 *
 * <p>{@code groupingBy(bucket, counting())} считает частоты; {@code bucket} заодно валидирует
 * отрицательные значения (и бросает NPE на {@code null}-элементе при распаковке). Затем итоговая
 * карта строится в порядке объявления корзин и через {@code getOrDefault} получает {@code 0L} для
 * пустых интервалов. Это гарантирует все четыре ключа и предсказуемый порядок.
 *
 * <p>Границы: 10 входит в первую корзину, 50 — во вторую, 200 — в третью, 201 — в {@code "200ms+"}.
 * Обёртка {@code unmodifiableMap} запрещает мутацию результата.
 */
public final class Easy20_HistogramBucketingSolution extends Easy20_HistogramBucketing {

  private static final List<String> BUCKETS = List.of("0-10ms", "11-50ms", "51-200ms", "200ms+");

  @Override
  public Map<String, Long> buildLatencyHistogram(List<Long> latenciesMs) {
    Objects.requireNonNull(latenciesMs, "latenciesMs");

    Map<String, Long> counts =
        latenciesMs.stream()
            .collect(
                Collectors.groupingBy(
                    Easy20_HistogramBucketingSolution::bucket, Collectors.counting()));

    Map<String, Long> histogram = new LinkedHashMap<>();
    BUCKETS.forEach(bucket -> histogram.put(bucket, counts.getOrDefault(bucket, 0L)));
    return Collections.unmodifiableMap(histogram);
  }

  private static String bucket(long latency) {
    if (latency < 0) {
      throw new IllegalArgumentException("latency must be >= 0, got " + latency);
    }
    if (latency <= 10) {
      return "0-10ms";
    }
    if (latency <= 50) {
      return "11-50ms";
    }
    if (latency <= 200) {
      return "51-200ms";
    }
    return "200ms+";
  }
}
