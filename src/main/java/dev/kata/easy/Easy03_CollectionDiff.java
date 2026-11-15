package dev.kata.easy;

import java.util.List;
import java.util.Objects;

/**
 * Easy 03. Diff двух коллекций по идентификатору (O(N + M)).
 *
 * <h2>Задача</h2>
 *
 * <p>Даны предыдущий и текущий списки пользователей. Сравнить их по {@code id} и вернуть {@link
 * DiffResult}:
 *
 * <ul>
 *   <li>{@code added} — есть в текущем, но не было в предыдущем;
 *   <li>{@code removed} — был в предыдущем, но отсутствует в текущем;
 *   <li>{@code unchanged} — {@code id} присутствует в обоих списках (берётся текущая версия).
 * </ul>
 *
 * <p>Порядок элементов внутри каждой категории повторяет порядок соответствующего входного списка.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * previous = [User("1","a@x"), User("2","b@x")]
 * current  = [User("2","b2@x"), User("3","c@x")]
 *
 * added     → [User("3","c@x")]
 * removed   → [User("1","a@x")]
 * unchanged → [User("2","b2@x")]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ размеры списков;
 *   <li>{@code id} и {@code email} не {@code null}/не blank.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(N + M), память O(N + M): индексы по {@code id} на {@link java.util.Set}, затем
 * декларативная фильтрация потоков. Вложенный перебор дал бы O(N × M). Коллекции результата
 * неизменяемы.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy03_CollectionDiffSolution}.
 */
public class Easy03_CollectionDiff {

  /** Пользователь. */
  public record User(String id, String email) {
    public User {
      requireText(id, "id");
      requireText(email, "email");
    }
  }

  /** Результат сравнения. Списки неизменяемы. */
  public record DiffResult<T>(List<T> added, List<T> removed, List<T> unchanged) {
    public DiffResult {
      added = List.copyOf(Objects.requireNonNull(added, "added"));
      removed = List.copyOf(Objects.requireNonNull(removed, "removed"));
      unchanged = List.copyOf(Objects.requireNonNull(unchanged, "unchanged"));
    }
  }

  /** Сравнивает списки пользователей по {@code id}. */
  public DiffResult<User> diff(List<User> previous, List<User> current) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
