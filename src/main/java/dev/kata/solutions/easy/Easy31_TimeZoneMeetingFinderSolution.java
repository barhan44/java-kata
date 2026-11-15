package dev.kata.solutions.easy;

import dev.kata.easy.Easy31_TimeZoneMeetingFinder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Эталон Easy 31.
 *
 * <p>{@code IntStream.range(0, 24)} создаёт кандидатов на каждый целый час UTC. Для каждого
 * кандидата {@code withZoneSameInstant} переводит момент в таймзону участника — это корректно
 * обрабатывает смещения и DST, в отличие от ручного {@code plusHours(offset)}. Окно проверяется как
 * {@code start <= local < end}, поэтому конечный час не включается.
 *
 * <p>{@code allMatch} по участникам короткозамыкает на первом несовпадении. Пустой список
 * участников даёт все 24 часа: ограничений нет. Возвращаемые {@link ZonedDateTime} имеют {@link
 * ZoneOffset#UTC} и дату {@code utcDate}.
 */
public final class Easy31_TimeZoneMeetingFinderSolution extends Easy31_TimeZoneMeetingFinder {

  @Override
  public List<ZonedDateTime> findAvailableUtcHours(
      List<Participant> participants, LocalDate utcDate) {
    Objects.requireNonNull(participants, "participants");
    Objects.requireNonNull(utcDate, "utcDate");

    return IntStream.range(0, 24)
        .mapToObj(hour -> ZonedDateTime.of(utcDate, LocalTime.of(hour, 0), ZoneOffset.UTC))
        .filter(
            candidate ->
                participants.stream().allMatch(participant -> isWorking(participant, candidate)))
        .toList();
  }

  private static boolean isWorking(Participant participant, ZonedDateTime utcHour) {
    LocalTime local = utcHour.withZoneSameInstant(participant.zone()).toLocalTime();
    return !local.isBefore(participant.workStart()) && local.isBefore(participant.workEnd());
  }
}
