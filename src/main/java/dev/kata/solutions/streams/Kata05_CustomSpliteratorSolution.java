package dev.kata.solutions.streams;

import dev.kata.streams.Kata05_CustomSpliterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Эталонное решение Kata 05.
 *
 * <h2>Почему именно Spliterator</h2>
 *
 * <p>{@link Spliterator} — единственный стандартный способ отдать ленивый источник в {@code
 * Stream}/{@code parallelStream} так, чтобы:
 *
 * <ul>
 *   <li>не материализовать промежуточную коллекцию;
 *   <li>получить полноценный {@code trySplit} для параллельного выполнения;
 *   <li>работать с любым размером входа за O(1) дополнительной памяти.
 * </ul>
 *
 * <h2>Устройство</h2>
 *
 * <p>Spliterator хранит ссылку на {@code List<String>} и изменяемый диапазон {@code [start, end)}.
 * {@link #trySplit()} не копирует данные: он делит диапазон пополам, создаёт новый spliterator на
 * префиксе и сдвигает {@code start} текущего на середину. Оба экземпляра затем работают независимо
 * — JDK гарантирует, что каждый из них в параллельном стриме обрабатывает один поток.
 *
 * <p>{@link #tryAdvance(Consumer)} читает по одной строке, пропускает пустые строки и комментарии,
 * парсит и передаёт результат в {@code action}. Курсор сдвигается до вызова парсера: при исключении
 * spliterator остаётся в валидном состоянии (следующий вызов продолжит со следующей строки), а не
 * зациклится на «плохой» строке.
 *
 * <h2>Характеристики</h2>
 *
 * <p>{@code ORDERED | NONNULL | IMMUTABLE}. {@code SIZED} не указан намеренно: оценка размера
 * включает пропускаемые строки и потому является верхней границей, а не точным размером. {@code
 * IMMUTABLE} корректен, поскольку мы не меняем исходный список (это ответственность вызывающего:
 * список должен быть read-only).
 *
 * <h2>Аллокации</h2>
 *
 * <p>На каждую валидную строку создаётся один {@link Reading} — это неизбежно, поскольку record
 * передаётся в стрим. Никаких промежуточных списков, {@code String.split} (он компилирует regex и
 * создаёт массив) и боксинга нет: используется {@code indexOf} и {@code substring}.
 */
public final class Kata05_CustomSpliteratorSolution extends Kata05_CustomSpliterator {

  private static final char SEPARATOR = ';';
  private static final char COMMENT = '#';

  public Kata05_CustomSpliteratorSolution(List<String> lines) {
    super(Objects.requireNonNull(lines, "lines"));
  }

  private Kata05_CustomSpliteratorSolution(List<String> lines, int start, int end) {
    super(lines, start, end);
  }

  @Override
  public boolean tryAdvance(Consumer<? super Reading> action) {
    Objects.requireNonNull(action, "action");

    while (start < end) {
      int lineNumber = start + 1;
      String raw = lines.get(start++);
      if (raw == null) {
        throw invalid(lineNumber, "null", "line is null");
      }

      String line = raw.trim();
      if (line.isEmpty() || line.charAt(0) == COMMENT) {
        continue;
      }

      action.accept(parse(line, lineNumber));
      return true;
    }
    return false;
  }

  @Override
  public Spliterator<Reading> trySplit() {
    int remaining = end - start;
    if (remaining <= 1) {
      // Дробить дальше нечего: один элемент или пустой диапазон.
      return null;
    }
    int mid = start + (remaining >>> 1);
    Spliterator<Reading> prefix = new Kata05_CustomSpliteratorSolution(lines, start, mid);
    this.start = mid; // текущий экземпляр продолжает работать с суффиксом
    return prefix;
  }

  @Override
  public long estimateSize() {
    return end - start;
  }

  @Override
  public int characteristics() {
    return ORDERED | NONNULL | IMMUTABLE;
  }

  private static Reading parse(String line, int lineNumber) {
    int separator = line.indexOf(SEPARATOR);
    if (separator < 0) {
      throw invalid(lineNumber, line, "missing ';' separator");
    }

    String timestampPart = line.substring(0, separator).trim();
    String valuePart = line.substring(separator + 1).trim();
    if (timestampPart.isEmpty() || valuePart.isEmpty()) {
      throw invalid(lineNumber, line, "empty timestamp or value");
    }

    long timestamp;
    double value;
    try {
      timestamp = Long.parseLong(timestampPart);
    } catch (NumberFormatException e) {
      throw invalid(lineNumber, line, "timestamp is not a long: " + timestampPart);
    }
    try {
      value = Double.parseDouble(valuePart);
    } catch (NumberFormatException e) {
      throw invalid(lineNumber, line, "value is not a double: " + valuePart);
    }
    if (!Double.isFinite(value)) {
      throw invalid(lineNumber, line, "value must be finite: " + value);
    }
    return new Reading(timestamp, value);
  }

  private static IllegalArgumentException invalid(int lineNumber, String line, String reason) {
    return new IllegalArgumentException(
        "Invalid line #" + lineNumber + " (\"" + line + "\"): " + reason);
  }
}
