package dev.kata.easy;

import java.util.Optional;

/**
 * Easy 29. Извлечение email через Optional pipeline.
 *
 * <h2>Задача</h2>
 *
 * <p>Дана вложенная nullable-структура {@code Company → Department → Manager → ContactInfo →
 * email}. Извлечь первый валидный email, не используя {@code if (x != null)}. Любой отсутствующий
 * уровень или невалидный email даёт {@link Optional#empty()}.
 *
 * <p>Валидным считается непустой после trim email ровно с одним {@code @}, непустыми локальной
 * частью и доменом, а также точкой внутри домена не в начале и не в конце.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * Company(Department(Manager(ContactInfo(" Alice@Example.COM "))))
 *   → Optional["Alice@Example.COM"]
 *
 * Company(null) → Optional.empty()
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <p>Любое поле структуры может быть {@code null}; сам {@code company} тоже может быть {@code
 * null}.
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(1), память O(1): цепочка {@code Optional.map} автоматически превращает {@code null} в
 * пустой Optional, {@code filter} отсекает blank и невалидные адреса. Возвращаемое значение —
 * {@link Optional}, мутаций нет.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy29_OptionalPipelineExtractorSolution}.
 */
public class Easy29_OptionalPipelineExtractor {

  /** Контактная информация. */
  public record ContactInfo(String email) {}

  /** Менеджер. */
  public record Manager(String name, ContactInfo contactInfo) {}

  /** Отдел. */
  public record Department(String name, Manager manager) {}

  /** Компания. */
  public record Company(String name, Department department) {}

  /** Возвращает валидный email или пустой Optional. */
  public Optional<String> extractEmail(Company company) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
