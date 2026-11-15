package dev.kata.easy;

import java.util.List;

/**
 * Easy 40. Самые частые префиксы URL.
 *
 * <h2>Задача</h2>
 *
 * <p>Обрезать каждый URL-путь до {@code depth} сегментов, посчитать частоту полученных префиксов и
 * вернуть {@code topK} самых частых. При равной частоте — по алфавиту. Префикс возвращается с
 * ведущим слешем: {@code "/api/v1"}. Пустой путь и путь из одних слешей дают префикс {@code "/"}.
 * Query-строка и fragment отбрасываются; пустые сегменты (двойные слеши) игнорируются.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * paths = ["/api/v1/users", "/api/v1/orders", "/api/v2/users", "/web/home"]
 * topPrefixSegments(paths, 2, 2) → ["/api/v1", "/api/v2"]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code paths.size()}; {@code depth ≥ 0}, {@code topK ≥ 0};
 *   <li>элементы путей не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(T + P log P), память O(P), где T — суммарное число сегментов, P — число уникальных
 * префиксов. Эталон нормализует путь, режет его через {@code Stream.limit(depth)}, группирует через
 * {@code groupingBy(counting())} и сортирует компаратором «частота ↓, префикс ↑». Результат
 * неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy40_MostFrequentUrlPrefixSolution}.
 */
public class Easy40_MostFrequentUrlPrefix {

  /** Возвращает топ-K префиксов путей. */
  public List<String> topPrefixSegments(List<String> paths, int depth, int topK) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
