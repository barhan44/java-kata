package dev.kata.solutions.easy;

import dev.kata.easy.Easy04_InvertedIndex;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Эталон Easy 04.
 *
 * <p>Индекс строится одним {@code flatMap} в пары {@code (tag, articleId)}. {@code groupingBy} с
 * {@code TreeMap} упорядочивает теги, а downstream собирает id в {@link LinkedHashSet} — порядок
 * статей сохраняется, дубликаты (если одна статья дважды указала тег) исчезают. Затем каждое
 * множество оборачивается unmodifiable, как и вся карта.
 *
 * <p>Поиск не перебирает все статьи: выбирается наименьшее множество по одному из тегов, и только
 * его id проверяются на наличие остальных тегов. Если хотя бы одного тега нет в индексе, результат
 * сразу пуст. Пустой набор требований тоже даёт пустой результат — в индексе нет информации о
 * статьях без тегов.
 */
public final class Easy04_InvertedIndexSolution extends Easy04_InvertedIndex {

  @Override
  public Map<String, Set<String>> buildIndex(List<Article> articles) {
    Objects.requireNonNull(articles, "articles");

    Map<String, Set<String>> index =
        articles.stream()
            .flatMap(article -> article.tags().stream().map(tag -> Map.entry(tag, article.id())))
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    TreeMap::new,
                    Collectors.mapping(
                        Map.Entry::getValue,
                        Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            Collections::unmodifiableSet))));
    return Collections.unmodifiableMap(index);
  }

  @Override
  public Set<String> findArticlesWithAllTags(
      Map<String, Set<String>> index, Set<String> requiredTags) {
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(requiredTags, "requiredTags");
    if (requiredTags.isEmpty() || !index.keySet().containsAll(requiredTags)) {
      return Set.of();
    }

    Set<String> smallest =
        requiredTags.stream().map(index::get).min(Comparator.comparingInt(Set::size)).orElseThrow();

    return smallest.stream()
        .filter(
            articleId -> requiredTags.stream().allMatch(tag -> index.get(tag).contains(articleId)))
        .collect(Collectors.toUnmodifiableSet());
  }
}
