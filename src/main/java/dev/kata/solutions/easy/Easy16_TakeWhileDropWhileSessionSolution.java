package dev.kata.solutions.easy;

import dev.kata.easy.Easy16_TakeWhileDropWhileSession;
import java.util.List;
import java.util.Objects;

/**
 * Эталон Easy 16.
 *
 * <p>{@code dropWhile} пропускает INFO/WARN до первой строки ERROR; сам ERROR остаётся в потоке,
 * потому что предикат «не ERROR» для него уже ложен. Затем {@code takeWhile} берёт строки, пока не
 * встретит RESOLVED, — сам RESOLVED не проходит. Такая композиция читается как «от первой ошибки до
 * первого разрешения» и не требует ручного поиска индексов. {@code toList()} отдаёт неизменяемый
 * список сообщений.
 *
 * <p>Если ERROR нет, {@code dropWhile} выедает весь поток; если RESOLVED нет, {@code takeWhile}
 * берёт всё до конца. Оба граничных случая обрабатываются без дополнительных условий.
 */
public final class Easy16_TakeWhileDropWhileSessionSolution
    extends Easy16_TakeWhileDropWhileSession {

  @Override
  public List<String> extractErrorSession(List<LogLine> logs) {
    Objects.requireNonNull(logs, "logs");

    return logs.stream()
        .dropWhile(line -> !line.level().equals("ERROR"))
        .takeWhile(line -> !line.level().equals("RESOLVED"))
        .map(LogLine::message)
        .toList();
  }
}
