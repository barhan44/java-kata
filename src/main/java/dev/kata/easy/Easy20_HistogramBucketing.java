package dev.kata.easy;

import java.util.List;
import java.util.Map;

/**
 * Easy 20. Гистограмма латентности по корзинам.
 *
 * <h2>Задача</h2>
 *
 * <p>По списку задержек в миллисекундах построить гистограмму с корзинами:
 *
 * <ul>
 *   <li>{@code "0-10ms"} — 0..10 включительно;
 *   <li>{@code "11-50ms"} — 11..50;
 *   <li>{@code "51-200ms"} — 51..200;
 *   <li>{@code "200ms+"} — больше 200.
 * </ul>
 *
 * <p>В результате всегда присутствуют все четыре ключа, даже если счётчик равен нулю.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * latencies = [0, 5, 10, 11, 60, 201]
 * histogram → {"0-10ms": 3, "11-50ms": 1, "51-200ms": 1, "200ms+": 1}
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code latenciesMs.size()}; значения ≥ 0.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(1) на четыре корзины. Эталон группирует через {@code groupingBy(...,
 * counting())}, затем заполняет отсутствующие корзины нулями в фиксированном порядке. Карта
 * неизменяема, порядок ключей стабилен.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy20_HistogramBucketingSolution}.
 */
public class Easy20_HistogramBucketing {

  /** Строит гистограмму с четырьмя гарантированными корзинами. */
  public Map<String, Long> buildLatencyHistogram(List<Long> latenciesMs) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
