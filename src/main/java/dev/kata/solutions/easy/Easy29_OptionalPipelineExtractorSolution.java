package dev.kata.solutions.easy;

import dev.kata.easy.Easy29_OptionalPipelineExtractor;
import java.util.Optional;

/**
 * Эталон Easy 29.
 *
 * <p>{@link Optional#map} возвращает пустой Optional, если функция вернула {@code null}, поэтому
 * цепочка безопасно проходит через отсутствующие Department/Manager/ContactInfo/email. После {@code
 * trim} blank отсекается фильтром, затем проверяется структура адреса: ровно один {@code @},
 * непустые части и точка внутри домена. Никаких {@code if (x != null)} и преждевременных {@code
 * Optional.get()}.
 *
 * <p>Возвращается trimmed-адрес с сохранением регистра; {@link Optional} — сам по себе контейнер,
 * дополнительной обёртки не нужно.
 */
public final class Easy29_OptionalPipelineExtractorSolution
    extends Easy29_OptionalPipelineExtractor {

  @Override
  public Optional<String> extractEmail(Company company) {
    return Optional.ofNullable(company)
        .map(Company::department)
        .map(Department::manager)
        .map(Manager::contactInfo)
        .map(ContactInfo::email)
        .map(String::trim)
        .filter(email -> !email.isEmpty())
        .filter(Easy29_OptionalPipelineExtractorSolution::isValidEmail);
  }

  private static boolean isValidEmail(String email) {
    if (email.chars().anyMatch(Character::isWhitespace)) {
      return false;
    }
    int at = email.indexOf('@');
    if (at <= 0 || at != email.lastIndexOf('@') || at == email.length() - 1) {
      return false;
    }
    String domain = email.substring(at + 1);
    int dot = domain.indexOf('.');
    return dot > 0 && dot < domain.length() - 1;
  }
}
