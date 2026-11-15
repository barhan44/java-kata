package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy17_MapMultiFlattener.ErrorEvent;
import dev.kata.easy.Easy17_MapMultiFlattener.Event;
import dev.kata.easy.Easy17_MapMultiFlattener.LoginEvent;
import dev.kata.easy.Easy17_MapMultiFlattener.PurchaseEvent;
import dev.kata.solutions.easy.Easy17_MapMultiFlattenerSolution;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 17: mapMulti event flattener")
class Easy17_MapMultiFlattenerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final Instant T = Instant.parse("2026-01-01T12:00:00Z");

  private Easy17_MapMultiFlattener newSolution() {
    return USE_SOLUTION ? new Easy17_MapMultiFlattenerSolution() : new Easy17_MapMultiFlattener();
  }

  @Test
  @DisplayName("Разворачивает все три типа событий")
  void flattensAllEventTypes() {
    List<Event> events =
        List.of(
            new LoginEvent("alice", T),
            new PurchaseEvent("bob", new BigDecimal("10.50"), T),
            new ErrorEvent("E500", "boom", T));

    assertThat(newSolution().auditLines(events))
        .containsExactly(
            "LOGIN alice @" + T, "PURCHASE bob 10.50 @" + T, "ERROR E500 @" + T, "MESSAGE boom");
  }

  @Test
  @DisplayName("Ошибка даёт две строки, порядок событий сохраняется")
  void errorExpandsToTwoLines() {
    List<Event> events =
        List.of(
            new ErrorEvent("E1", "first", T),
            new LoginEvent("carol", T),
            new ErrorEvent("E2", "second", T));

    assertThat(newSolution().auditLines(events))
        .containsExactly(
            "ERROR E1 @" + T,
            "MESSAGE first",
            "LOGIN carol @" + T,
            "ERROR E2 @" + T,
            "MESSAGE second");
  }

  @Test
  @DisplayName("Пустой список")
  void emptyList() {
    assertThat(newSolution().auditLines(List.of())).isEmpty();
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<String> lines = newSolution().auditLines(List.of(new LoginEvent("alice", T)));
    assertThatThrownBy(() -> lines.add("extra")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация событий и null-списка")
  void validation() {
    assertThatThrownBy(() -> new LoginEvent(null, T)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new LoginEvent(" ", T)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LoginEvent("alice", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new PurchaseEvent("bob", null, T))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ErrorEvent("E1", " ", T))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().auditLines(null))
        .isInstanceOf(NullPointerException.class);
  }
}
