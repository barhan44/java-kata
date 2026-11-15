package dev.kata.easy;

import java.util.List;

/**
 * Easy 27. Префиксное дерево и autocomplete.
 *
 * <h2>Задача</h2>
 *
 * <p>Реализовать {@link PrefixDictionary}: {@code insert(word)} добавляет слово, {@code
 * suggest(prefix, limit)} возвращает до {@code limit} слов, начинающихся с префикса, в
 * лексикографическом порядке. Пустой префикс означает «все слова»; {@code limit == 0} — пустой
 * результат.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * insert("car"); insert("cart"); insert("carbon"); insert("care"); insert("dog")
 *
 * suggest("car", 10) → ["car","carbon","care","cart"]
 * suggest("ca", 2)   → ["car","carbon"]
 * suggest("do", 10)  → ["dog"]
 * suggest("x", 10)   → []
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>слова и префиксы не {@code null}; слова не blank;
 *   <li>{@code limit ≥ 0}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>{@code insert} — O(L), {@code suggest} — O(P + S log S), где P — длина префикса, S — число
 * найденных слов; память O(total characters). Узлы хранят {@code Map<Character, Node>}, новые ветки
 * создаются через {@code computeIfAbsent}. Результат неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy27_TrieAutocompletePrefixSolution}.
 */
public class Easy27_TrieAutocompletePrefix {

  /** Префиксный словарь. */
  public static class PrefixDictionary {

    protected PrefixDictionary() {}

    /** Добавляет слово. */
    public void insert(String word) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Возвращает до {@code limit} слов с указанным префиксом. */
    public List<String> suggest(String prefix, int limit) {
      throw new UnsupportedOperationException("Not implemented yet");
    }
  }

  /** Фабрика словаря: тесты выбирают заготовку или эталон. */
  public PrefixDictionary newPrefixDictionary() {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
