package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy16_TakeWhileDropWhileSession.LogLine;
import dev.kata.solutions.easy.Easy16_TakeWhileDropWhileSessionSolution;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 16: Error session with dropWhile/takeWhile")
class Easy16_TakeWhileDropWhileSessionTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

  private Easy16_TakeWhileDropWhileSession newSolution() {
    return USE_SOLUTION
        ? new Easy16_TakeWhileDropWhileSessionSolution()
        : new Easy16_TakeWhileDropWhileSession();
  }

  @Test
  @DisplayName("Берёт сообщения от ERROR до RESOLVED, не включая RESOLVED")
  void extractsSession() {
    List<LogLine> logs =
        List.of(
            line(0, "INFO", "start"),
            line(1, "ERROR", "db down"),
            line(2, "WARN", "retry"),
            line(3, "RESOLVED", "db up"),
            line(4, "ERROR", "later"));

    assertThat(newSolution().extractErrorSession(logs)).containsExactly("db down", "retry");
  }

  @Test
  @DisplayName("ERROR и RESOLVED подряд дают только ERROR")
  void immediateResolve() {
    List<LogLine> logs =
        List.of(line(0, "INFO", "start"), line(1, "ERROR", "boom"), line(2, "RESOLVED", "ok"));

    assertThat(newSolution().extractErrorSession(logs)).containsExactly("boom");
  }

  @Test
  @DisplayName("Без ERROR результат пуст")
  void noError() {
    List<LogLine> logs =
        List.of(line(0, "INFO", "a"), line(1, "WARN", "b"), line(2, "RESOLVED", "c"));
    assertThat(newSolution().extractErrorSession(logs)).isEmpty();
    assertThat(newSolution().extractErrorSession(List.of())).isEmpty();
  }

  @Test
  @DisplayName("Без RESOLVED берутся все сообщения до конца")
  void noResolved() {
    List<LogLine> logs =
        List.of(line(0, "INFO", "start"), line(1, "ERROR", "a"), line(2, "INFO", "b"));
    assertThat(newSolution().extractErrorSession(logs)).containsExactly("a", "b");
  }

  @Test
  @DisplayName("RESOLVED до ERROR игнорируется")
  void resolvedBeforeError() {
    List<LogLine> logs = List.of(line(0, "RESOLVED", "old"), line(1, "ERROR", "new"));
    assertThat(newSolution().extractErrorSession(logs)).containsExactly("new");
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<String> result = newSolution().extractErrorSession(List.of(line(0, "ERROR", "x")));
    assertThatThrownBy(() -> result.add("y")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация LogLine и null-списка")
  void validation() {
    assertThatThrownBy(() -> new LogLine(null, "INFO", "m"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new LogLine(T0, " ", "m"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LogLine(T0, "INFO", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().extractErrorSession(null))
        .isInstanceOf(NullPointerException.class);
  }

  private static LogLine line(int second, String level, String message) {
    return new LogLine(T0.plusSeconds(second), level, message);
  }
}
