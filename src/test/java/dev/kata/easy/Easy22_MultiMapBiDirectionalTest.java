package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy22_MultiMapBiDirectional.BiDirectionalLookup;
import dev.kata.solutions.easy.Easy22_MultiMapBiDirectionalSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 22: Bi-directional lookup")
class Easy22_MultiMapBiDirectionalTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy22_MultiMapBiDirectional newSolution() {
    return USE_SOLUTION
        ? new Easy22_MultiMapBiDirectionalSolution()
        : new Easy22_MultiMapBiDirectional();
  }

  @Test
  @DisplayName("Хранит связь в обе стороны")
  void storesBothDirections() {
    BiDirectionalLookup<String, Integer> lookup = newSolution().newLookup();

    lookup.put("a", 1);
    lookup.put("b", 2);

    assertThat(lookup.getValue("a")).contains(1);
    assertThat(lookup.getKey(2)).contains("b");
    assertThat(lookup.size()).isEqualTo(2);
    assertThat(lookup.getValue("missing")).isEmpty();
    assertThat(lookup.getKey(99)).isEmpty();
  }

  @Test
  @DisplayName("Перезапись ключа удаляет старую обратную связь")
  void overwritingKeyRemovesOldValueMapping() {
    BiDirectionalLookup<String, Integer> lookup = newSolution().newLookup();
    lookup.put("a", 1);

    lookup.put("a", 2);

    assertThat(lookup.getValue("a")).contains(2);
    assertThat(lookup.getKey(2)).contains("a");
    assertThat(lookup.getKey(1)).isEmpty();
    assertThat(lookup.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("Перезапись значения удаляет старую прямую связь")
  void overwritingValueRemovesOldKeyMapping() {
    BiDirectionalLookup<String, Integer> lookup = newSolution().newLookup();
    lookup.put("a", 1);

    lookup.put("b", 1);

    assertThat(lookup.getValue("b")).contains(1);
    assertThat(lookup.getKey(1)).contains("b");
    assertThat(lookup.getValue("a")).isEmpty();
    assertThat(lookup.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("Конфликт по ключу и значению одновременно")
  void bothConflicts() {
    BiDirectionalLookup<String, Integer> lookup = newSolution().newLookup();
    lookup.put("a", 1);
    lookup.put("b", 2);

    lookup.put("a", 2);

    assertThat(lookup.getValue("a")).contains(2);
    assertThat(lookup.getKey(2)).contains("a");
    assertThat(lookup.getValue("b")).isEmpty();
    assertThat(lookup.getKey(1)).isEmpty();
    assertThat(lookup.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("Повторная вставка той же пары идемпотентна")
  void samePairTwice() {
    BiDirectionalLookup<String, Integer> lookup = newSolution().newLookup();
    lookup.put("a", 1);
    lookup.put("a", 1);

    assertThat(lookup.getValue("a")).contains(1);
    assertThat(lookup.getKey(1)).contains("a");
    assertThat(lookup.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("null-ключи и значения отклоняются")
  void validation() {
    BiDirectionalLookup<String, Integer> lookup = newSolution().newLookup();

    assertThatThrownBy(() -> lookup.put(null, 1)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> lookup.put("a", null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> lookup.getValue(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> lookup.getKey(null)).isInstanceOf(NullPointerException.class);
  }
}
