package dev.kata.solutions.easy;

import dev.kata.easy.Easy13_ToMapConflictResolution;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Эталон Easy 13.
 *
 * <p>Первый {@code toMap} хранит не строку, а весь {@link ConfigEntry}: merge-функция получает оба
 * кандидата и может сравнить priority и updatedAt. Побеждает больший priority; при равенстве —
 * более позднее время; при полном равенстве возвращается существующая запись, то есть первая по
 * порядку входа. Второй {@code toMap} превращает победителей в {@code key → value}, используя
 * {@link TreeMap} для детерминированного порядка ключей.
 *
 * <p>Обёртка {@code unmodifiableMap} запрещает менять результат.
 */
public final class Easy13_ToMapConflictResolutionSolution extends Easy13_ToMapConflictResolution {

  @Override
  public Map<String, String> resolve(List<ConfigEntry> entries) {
    Objects.requireNonNull(entries, "entries");

    Map<String, ConfigEntry> winners =
        entries.stream()
            .collect(
                Collectors.toMap(
                    ConfigEntry::key,
                    Function.identity(),
                    Easy13_ToMapConflictResolutionSolution::prefer));

    Map<String, String> resolved =
        winners.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().value(),
                    (left, right) -> left,
                    TreeMap::new));
    return Collections.unmodifiableMap(resolved);
  }

  private static ConfigEntry prefer(ConfigEntry existing, ConfigEntry replacement) {
    int byPriority = Integer.compare(replacement.priority(), existing.priority());
    if (byPriority != 0) {
      return byPriority > 0 ? replacement : existing;
    }
    int byTime = replacement.updatedAt().compareTo(existing.updatedAt());
    if (byTime != 0) {
      return byTime > 0 ? replacement : existing;
    }
    return existing;
  }
}
