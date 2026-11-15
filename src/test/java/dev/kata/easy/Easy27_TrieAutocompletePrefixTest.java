package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy27_TrieAutocompletePrefix.PrefixDictionary;
import dev.kata.solutions.easy.Easy27_TrieAutocompletePrefixSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 27: Trie autocomplete")
class Easy27_TrieAutocompletePrefixTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy27_TrieAutocompletePrefix newSolution() {
    return USE_SOLUTION
        ? new Easy27_TrieAutocompletePrefixSolution()
        : new Easy27_TrieAutocompletePrefix();
  }

  private PrefixDictionary dictionaryWith(String... words) {
    PrefixDictionary dictionary = newSolution().newPrefixDictionary();
    for (String word : words) {
      dictionary.insert(word);
    }
    return dictionary;
  }

  @Test
  @DisplayName("Предлагает слова с префиксом в алфавитном порядке")
  void suggestsByPrefix() {
    PrefixDictionary dictionary = dictionaryWith("car", "cart", "carbon", "care", "dog");

    assertThat(dictionary.suggest("car", 10)).containsExactly("car", "carbon", "care", "cart");
    assertThat(dictionary.suggest("ca", 2)).containsExactly("car", "carbon");
    assertThat(dictionary.suggest("do", 10)).containsExactly("dog");
  }

  @Test
  @DisplayName("Пустой префикс отдаёт все слова, limit ограничивает")
  void emptyPrefixAndLimit() {
    PrefixDictionary dictionary = dictionaryWith("car", "carbon", "care", "cart", "do", "dog");

    assertThat(dictionary.suggest("", 3)).containsExactly("car", "carbon", "care");
    assertThat(dictionary.suggest("", 100))
        .containsExactly("car", "carbon", "care", "cart", "do", "dog");
    assertThat(dictionary.suggest("car", 0)).isEmpty();
  }

  @Test
  @DisplayName("Отсутствующий префикс и пустой словарь")
  void missingPrefix() {
    assertThat(dictionaryWith("car").suggest("x", 10)).isEmpty();
    assertThat(newSolution().newPrefixDictionary().suggest("", 10)).isEmpty();
  }

  @Test
  @DisplayName("Повторная вставка не создаёт дубликат")
  void duplicateInsert() {
    PrefixDictionary dictionary = dictionaryWith("car", "car");
    assertThat(dictionary.suggest("car", 10)).containsExactly("car");
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<String> result = dictionaryWith("car").suggest("car", 10);
    assertThatThrownBy(() -> result.add("cart")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация вставки и поиска")
  void validation() {
    PrefixDictionary dictionary = newSolution().newPrefixDictionary();

    assertThatThrownBy(() -> dictionary.insert(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dictionary.insert(" ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> dictionary.suggest(null, 1)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> dictionary.suggest("car", -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
