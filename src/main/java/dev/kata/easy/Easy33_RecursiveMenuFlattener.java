package dev.kata.easy;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Easy 33. Рекурсивное разворачивание меню.
 *
 * <h2>Задача</h2>
 *
 * <p>Дано дерево пунктов меню. Развернуть его в плоскую карту {@code code → полный путь}, где путь
 * строится из заголовков через разделитель {@code " / "}. Порядок — depth-first pre-order, то есть
 * пункт идёт перед своими детьми, дети — в порядке объявления.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * Главная (home)
 * └── Каталог (catalog)
 *     └── Ноутбуки (laptops)
 *
 * flattenMenuPaths → {
 *   "home":    "Главная",
 *   "catalog": "Главная / Каталог",
 *   "laptops": "Главная / Каталог / Ноутбуки"
 * }
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ число корней и глубина дерева;
 *   <li>{@code code} и {@code title} не {@code null}/не blank, {@code children} не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(n). Эталон — рекурсивный {@code Stream.flatMap}: пункт и его дети
 * склеиваются через {@code Stream.concat}, затем плоский поток собирается в {@link
 * java.util.LinkedHashMap}. Ручного обхода с индексами нет. Карта неизменяема.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy33_RecursiveMenuFlattenerSolution}.
 */
public class Easy33_RecursiveMenuFlattener {

  /** Пункт меню. */
  public record MenuItem(String code, String title, List<MenuItem> children) {
    public MenuItem {
      requireText(code, "code");
      requireText(title, "title");
      children = List.copyOf(Objects.requireNonNull(children, "children"));
    }
  }

  /** Разворачивает дерево в {@code code → полный путь}. */
  public Map<String, String> flattenMenuPaths(List<MenuItem> roots) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
