package dev.kata.easy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Easy 04. Обратный индекс по тегам (Map + Set).
 *
 * <h2>Задача</h2>
 *
 * <p>Даны статьи с наборами тегов. Построить индекс {@code tag → Set<articleId>} и реализовать
 * поиск статей, содержащих все требуемые теги (пересечение множеств).
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * articles = [
 *   Article("a1", Set.of("java", "backend")),
 *   Article("a2", Set.of("java")),
 *   Article("a3", Set.of("frontend"))
 * ]
 *
 * buildIndex → {"backend": ["a1"], "frontend": ["a3"], "java": ["a1","a2"]}
 * findArticlesWithAllTags(index, Set.of("java","backend")) → ["a1"]
 * findArticlesWithAllTags(index, Set.of("java","frontend")) → []
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ число статей; теги — непустые строки;
 *   <li>{@code requiredTags} может быть пустым — результат пуст.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Построение O(T), где T — суммарное число тегов. Поиск начинается с самого маленького множества
 * и проверяет остальные теги: O(min|Sᵢ| × |requiredTags|) проверок принадлежности. Возвращаемые
 * карта и множества неизменяемы.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy04_InvertedIndexSolution}.
 */
public class Easy04_InvertedIndex {

  /** Статья с уникальными тегами. */
  public record Article(String id, Set<String> tags) {
    public Article {
      requireText(id, "id");
      Objects.requireNonNull(tags, "tags");
      tags = Set.copyOf(tags);
    }
  }

  /** Строит индекс {@code tag → id статей}. */
  public Map<String, Set<String>> buildIndex(List<Article> articles) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /** Возвращает id статей, содержащих все теги из {@code requiredTags}. */
  public Set<String> findArticlesWithAllTags(
      Map<String, Set<String>> index, Set<String> requiredTags) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
