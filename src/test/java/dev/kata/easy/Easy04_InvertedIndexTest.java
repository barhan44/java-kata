package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy04_InvertedIndex.Article;
import dev.kata.solutions.easy.Easy04_InvertedIndexSolution;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 04: Inverted index by tags")
class Easy04_InvertedIndexTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy04_InvertedIndex newSolution() {
    return USE_SOLUTION ? new Easy04_InvertedIndexSolution() : new Easy04_InvertedIndex();
  }

  private static List<Article> sampleArticles() {
    return List.of(
        new Article("a1", Set.of("java", "backend")),
        new Article("a2", Set.of("java")),
        new Article("a3", Set.of("frontend")),
        new Article("a4", Set.of()));
  }

  @Test
  @DisplayName("Индекс строится по всем тегам и упорядочен")
  void buildsIndex() {
    Map<String, Set<String>> index = newSolution().buildIndex(sampleArticles());

    assertThat(index.keySet()).containsExactly("backend", "frontend", "java");
    assertThat(index.get("backend")).containsExactly("a1");
    assertThat(index.get("java")).containsExactly("a1", "a2");
    assertThat(index.get("frontend")).containsExactly("a3");
    assertThat(index).doesNotContainKey("missing");
  }

  @Test
  @DisplayName("Поиск по всем тегам — пересечение множеств")
  void findsIntersection() {
    Map<String, Set<String>> index = newSolution().buildIndex(sampleArticles());

    assertThat(newSolution().findArticlesWithAllTags(index, Set.of("java", "backend")))
        .containsExactly("a1");
    assertThat(newSolution().findArticlesWithAllTags(index, Set.of("java")))
        .containsExactlyInAnyOrder("a1", "a2");
    assertThat(newSolution().findArticlesWithAllTags(index, Set.of("java", "frontend"))).isEmpty();
    assertThat(newSolution().findArticlesWithAllTags(index, Set.of("unknown"))).isEmpty();
  }

  @Test
  @DisplayName("Пустые требования и пустой индекс")
  void emptyCases() {
    Map<String, Set<String>> index = newSolution().buildIndex(sampleArticles());

    assertThat(newSolution().findArticlesWithAllTags(index, Set.of())).isEmpty();
    assertThat(newSolution().findArticlesWithAllTags(Map.of(), Set.of("java"))).isEmpty();
    assertThat(newSolution().buildIndex(List.of())).isEmpty();
  }

  @Test
  @DisplayName("Дубликаты тегов не создают дубликатов id")
  void duplicateTags() {
    List<Article> articles =
        List.of(new Article("a1", Set.of("java")), new Article("a1", Set.of("java", "backend")));

    Map<String, Set<String>> index = newSolution().buildIndex(articles);

    assertThat(index.get("java")).containsExactly("a1");
  }

  @Test
  @DisplayName("Индекс и множества неизменяемы")
  void immutableResults() {
    Map<String, Set<String>> index = newSolution().buildIndex(sampleArticles());

    assertThatThrownBy(() -> index.put("new", Set.of("a5")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> index.get("java").add("a5"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Article копирует теги, валидация аргументов")
  void validation() {
    java.util.Set<String> sourceTags = new java.util.HashSet<>(Set.of("java"));
    Article article = new Article("a1", sourceTags);
    sourceTags.add("backend");
    assertThat(article.tags()).containsExactly("java");

    assertThatThrownBy(() -> new Article(null, Set.of())).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Article(" ", Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Article("a1", null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().buildIndex(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().findArticlesWithAllTags(null, Set.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().findArticlesWithAllTags(Map.of(), null))
        .isInstanceOf(NullPointerException.class);

    Map<String, Set<String>> mutableIndex = new HashMap<>();
    mutableIndex.put("java", new java.util.LinkedHashSet<>(Set.of("a1")));
    assertThat(newSolution().findArticlesWithAllTags(mutableIndex, Set.of("java")))
        .containsExactly("a1");
  }
}
