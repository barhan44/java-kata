package dev.kata.solutions.easy;

import dev.kata.easy.Easy27_TrieAutocompletePrefix;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Эталон Easy 27.
 *
 * <p>Узел дерева содержит {@code Map<Character, Node>} детей и флаг {@code terminal}. При вставке
 * для каждого символа вызывается {@code computeIfAbsent} — ветка создаётся ровно один раз, а
 * повторная вставка слова просто выставляет флаг. Поиск спускается по префиксу; если ветки нет,
 * сразу возвращается пустой список.
 *
 * <p>Сбор слов — рекурсивный обход поддерева с {@link StringBuilder}; затем слова сортируются
 * лексикографически и ограничиваются {@code limit}. Сортировка после сбора не зависит от порядка
 * вставки. {@code toList()} отдаёт неизменяемый результат.
 */
public final class Easy27_TrieAutocompletePrefixSolution extends Easy27_TrieAutocompletePrefix {

  @Override
  public PrefixDictionary newPrefixDictionary() {
    return new TrieDictionary();
  }

  private static final class TrieDictionary extends PrefixDictionary {

    private final Node root = new Node();

    @Override
    public void insert(String word) {
      Objects.requireNonNull(word, "word");
      if (word.isBlank()) {
        throw new IllegalArgumentException("word must not be blank");
      }

      Node node = root;
      for (char symbol : word.toCharArray()) {
        node = node.children.computeIfAbsent(symbol, ignored -> new Node());
      }
      node.terminal = true;
    }

    @Override
    public List<String> suggest(String prefix, int limit) {
      Objects.requireNonNull(prefix, "prefix");
      if (limit < 0) {
        throw new IllegalArgumentException("limit must be >= 0, got " + limit);
      }
      if (limit == 0) {
        return List.of();
      }

      Node node = root;
      for (char symbol : prefix.toCharArray()) {
        node = node.children.get(symbol);
        if (node == null) {
          return List.of();
        }
      }

      List<String> words = new ArrayList<>();
      collect(node, new StringBuilder(prefix), words);
      return words.stream().sorted().limit(limit).toList();
    }

    private static void collect(Node node, StringBuilder prefix, List<String> words) {
      if (node.terminal) {
        words.add(prefix.toString());
      }
      node.children.forEach(
          (symbol, child) -> {
            prefix.append(symbol);
            collect(child, prefix, words);
            prefix.deleteCharAt(prefix.length() - 1);
          });
    }
  }

  private static final class Node {
    private final Map<Character, Node> children = new HashMap<>();
    private boolean terminal;
  }
}
