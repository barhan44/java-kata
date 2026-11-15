package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy13_ToMapConflictResolution.ConfigEntry;
import dev.kata.solutions.easy.Easy13_ToMapConflictResolutionSolution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 13: toMap conflict resolution")
class Easy13_ToMapConflictResolutionTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final Instant T1 = Instant.parse("2026-01-01T10:00:00Z");
  private static final Instant T2 = Instant.parse("2026-01-01T11:00:00Z");

  private Easy13_ToMapConflictResolution newSolution() {
    return USE_SOLUTION
        ? new Easy13_ToMapConflictResolutionSolution()
        : new Easy13_ToMapConflictResolution();
  }

  @Test
  @DisplayName("Побеждает более высокий priority, даже если запись старее")
  void priorityWinsOverTime() {
    List<ConfigEntry> entries =
        List.of(new ConfigEntry("timeout", "1s", 1, T2), new ConfigEntry("timeout", "5s", 2, T1));

    assertThat(newSolution().resolve(entries)).containsEntry("timeout", "5s");
  }

  @Test
  @DisplayName("При равном priority побеждает более свежая запись")
  void timeBreaksTie() {
    List<ConfigEntry> entries =
        List.of(new ConfigEntry("timeout", "old", 1, T1), new ConfigEntry("timeout", "new", 1, T2));

    assertThat(newSolution().resolve(entries)).containsEntry("timeout", "new");
  }

  @Test
  @DisplayName("При полном равенстве остаётся первая запись")
  void firstWinsOnFullTie() {
    List<ConfigEntry> entries =
        List.of(new ConfigEntry("flag", "first", 1, T1), new ConfigEntry("flag", "second", 1, T1));

    assertThat(newSolution().resolve(entries)).containsEntry("flag", "first");
  }

  @Test
  @DisplayName("Разные ключи сохраняются, результат упорядочен и неизменяем")
  void independentKeysAndImmutability() {
    Map<String, String> result =
        newSolution()
            .resolve(List.of(new ConfigEntry("b", "2", 1, T1), new ConfigEntry("a", "1", 1, T1)));

    assertThat(result).containsEntry("a", "1").containsEntry("b", "2");
    assertThat(result.keySet()).containsExactly("a", "b");
    assertThatThrownBy(() -> result.put("c", "3"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Пустой список")
  void emptyList() {
    assertThat(newSolution().resolve(List.of())).isEmpty();
  }

  @Test
  @DisplayName("Валидация ConfigEntry и null-списка")
  void validation() {
    assertThatThrownBy(() -> new ConfigEntry(null, "v", 1, T1))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ConfigEntry(" ", "v", 1, T1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ConfigEntry("k", null, 1, T1))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ConfigEntry("k", "v", 1, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().resolve(null)).isInstanceOf(NullPointerException.class);
  }
}
