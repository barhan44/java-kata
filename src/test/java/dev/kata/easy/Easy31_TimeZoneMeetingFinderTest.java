package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy31_TimeZoneMeetingFinder.Participant;
import dev.kata.solutions.easy.Easy31_TimeZoneMeetingFinderSolution;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 31: Time zone meeting finder")
class Easy31_TimeZoneMeetingFinderTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final LocalDate DATE = LocalDate.of(2026, 1, 15);
  private static final LocalTime NINE = LocalTime.of(9, 0);
  private static final LocalTime SEVENTEEN = LocalTime.of(17, 0);

  private Easy31_TimeZoneMeetingFinder newSolution() {
    return USE_SOLUTION
        ? new Easy31_TimeZoneMeetingFinderSolution()
        : new Easy31_TimeZoneMeetingFinder();
  }

  @Test
  @DisplayName("UTC-участник: окно 09:00–17:00 даёт часы 9..16")
  void utcParticipant() {
    Participant participant = new Participant("UTC", ZoneOffset.UTC, NINE, SEVENTEEN);

    List<ZonedDateTime> result = newSolution().findAvailableUtcHours(List.of(participant), DATE);

    assertThat(result)
        .containsExactlyElementsOf(
            IntStream.range(9, 17)
                .mapToObj(hour -> ZonedDateTime.of(DATE, LocalTime.of(hour, 0), ZoneOffset.UTC))
                .toList());
  }

  @Test
  @DisplayName("Пересечение Берлина и Нью-Йорка зимой")
  void intersectionAcrossZones() {
    List<Participant> participants =
        List.of(
            new Participant("Berlin", ZoneId.of("Europe/Berlin"), NINE, SEVENTEEN),
            new Participant("NY", ZoneId.of("America/New_York"), NINE, SEVENTEEN));

    assertThat(newSolution().findAvailableUtcHours(participants, DATE))
        .containsExactly(
            ZonedDateTime.of(DATE, LocalTime.of(14, 0), ZoneOffset.UTC),
            ZonedDateTime.of(DATE, LocalTime.of(15, 0), ZoneOffset.UTC));
  }

  @Test
  @DisplayName("Нет пересечения окон")
  void noOverlap() {
    List<Participant> participants =
        List.of(
            new Participant("A", ZoneOffset.UTC, LocalTime.of(0, 0), LocalTime.of(1, 0)),
            new Participant("B", ZoneOffset.UTC, LocalTime.of(2, 0), LocalTime.of(3, 0)));

    assertThat(newSolution().findAvailableUtcHours(participants, DATE)).isEmpty();
  }

  @Test
  @DisplayName("Пустой список участников — все 24 часа")
  void noParticipantsAllHours() {
    assertThat(newSolution().findAvailableUtcHours(List.of(), DATE)).hasSize(24);
  }

  @Test
  @DisplayName("Границы окна: start включён, end исключён")
  void windowBoundaries() {
    Participant participant =
        new Participant("UTC", ZoneOffset.UTC, LocalTime.of(9, 0), LocalTime.of(10, 0));

    assertThat(newSolution().findAvailableUtcHours(List.of(participant), DATE))
        .containsExactly(ZonedDateTime.of(DATE, LocalTime.of(9, 0), ZoneOffset.UTC));
  }

  @Test
  @DisplayName("Валидация Participant и null-аргументов")
  void validation() {
    assertThatThrownBy(() -> new Participant("", ZoneOffset.UTC, NINE, SEVENTEEN))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Participant("A", null, NINE, SEVENTEEN))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Participant("A", ZoneOffset.UTC, null, SEVENTEEN))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Participant("A", ZoneOffset.UTC, NINE, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Participant("A", ZoneOffset.UTC, SEVENTEEN, NINE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().findAvailableUtcHours(null, DATE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                newSolution()
                    .findAvailableUtcHours(
                        List.of(new Participant("UTC", ZoneOffset.UTC, NINE, SEVENTEEN)), null))
        .isInstanceOf(NullPointerException.class);
  }
}
