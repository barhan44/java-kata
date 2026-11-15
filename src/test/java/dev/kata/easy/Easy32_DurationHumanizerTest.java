package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy32_DurationHumanizerSolution;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 32: Duration humanizer")
class Easy32_DurationHumanizerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy32_DurationHumanizer newSolution() {
    return USE_SOLUTION ? new Easy32_DurationHumanizerSolution() : new Easy32_DurationHumanizer();
  }

  @Test
  @DisplayName("Нулевая длительность")
  void zero() {
    assertThat(newSolution().formatDuration(Duration.ZERO)).isEqualTo("0s");
  }

  @Test
  @DisplayName("Только секунды, минуты и часы")
  void partialParts() {
    assertThat(newSolution().formatDuration(Duration.ofSeconds(30))).isEqualTo("30s");
    assertThat(newSolution().formatDuration(Duration.ofSeconds(90))).isEqualTo("1m 30s");
    assertThat(newSolution().formatDuration(Duration.ofHours(4).plusMinutes(5))).isEqualTo("4h 5m");
    assertThat(newSolution().formatDuration(Duration.ofDays(1))).isEqualTo("1d");
  }

  @Test
  @DisplayName("Полный формат из всех компонентов")
  void allParts() {
    Duration duration = Duration.ofDays(2).plusHours(4).plusMinutes(15).plusSeconds(30);

    assertThat(newSolution().formatDuration(duration)).isEqualTo("2d 4h 15m 30s");
  }

  @Test
  @DisplayName("Отрицательная длительность и доли секунды")
  void negativeAndSubSecond() {
    assertThat(newSolution().formatDuration(Duration.ofSeconds(-90))).isEqualTo("-1m 30s");
    assertThat(newSolution().formatDuration(Duration.ofMillis(500))).isEqualTo("0s");
  }

  @Test
  @DisplayName("null отклоняется")
  void nullDuration() {
    assertThatThrownBy(() -> newSolution().formatDuration(null))
        .isInstanceOf(NullPointerException.class);
  }
}
