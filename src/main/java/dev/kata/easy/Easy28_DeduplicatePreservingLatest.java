package dev.kata.easy;

import java.util.List;
import java.util.Objects;

/**
 * Easy 28. Дедупликация с сохранением последнего вхождения.
 *
 * <h2>Задача</h2>
 *
 * <p>Дан список сообщений с идентификаторами. Оставить для каждого {@code id} последнее вхождение
 * (самый свежий payload) и вернуть результат в порядке появления этих последних вхождений.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * messages = [Message("a","old"), Message("b","x"),
 *             Message("a","new"), Message("c","y"),
 *             Message("b","z")]
 *
 * deduplicate → [Message("a","new"), Message("c","y"), Message("b","z")]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code messages.size()};
 *   <li>{@code id} не {@code null}/не blank, {@code payload} не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n), память O(k), где k — число уникальных id. Эталон использует {@link
 * java.util.LinkedHashMap} как {@code SequencedMap}: при повторе ключ удаляется и добавляется через
 * {@code putLast}, поэтому порядок отражает последние вхождения. Возвращаемый список неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy28_DeduplicatePreservingLatestSolution}.
 */
public class Easy28_DeduplicatePreservingLatest {

  /** Сообщение. */
  public record Message(String id, String payload) {
    public Message {
      Objects.requireNonNull(id, "id");
      if (id.isBlank()) {
        throw new IllegalArgumentException("id must not be blank");
      }
      Objects.requireNonNull(payload, "payload");
    }
  }

  /** Оставляет последнее сообщение для каждого id в порядке последних вхождений. */
  public List<Message> deduplicate(List<Message> messages) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
