package dev.kata.solutions.easy;

import dev.kata.easy.Easy28_DeduplicatePreservingLatest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Эталон Easy 28.
 *
 * <p>{@code LinkedHashMap} реализует {@code SequencedMap}. При обработке сообщения старый ключ
 * удаляется, а новый вставляется через {@code putLast} — в итоге порядок ключей соответствует
 * последним вхождениям, а значение всегда самое свежее. Просто {@code put} без удаления оставил бы
 * порядок первых вхождений, что противоречит формулировке.
 *
 * <p>{@code List.copyOf(latest.values())} создаёт неизменяемый снимок значений; исходный список не
 * меняется.
 */
public final class Easy28_DeduplicatePreservingLatestSolution
    extends Easy28_DeduplicatePreservingLatest {

  @Override
  public List<Message> deduplicate(List<Message> messages) {
    Objects.requireNonNull(messages, "messages");

    LinkedHashMap<String, Message> latest = new LinkedHashMap<>();
    messages.forEach(
        message -> {
          Objects.requireNonNull(message, "message");
          latest.remove(message.id());
          latest.putLast(message.id(), message);
        });
    return List.copyOf(latest.values());
  }
}
