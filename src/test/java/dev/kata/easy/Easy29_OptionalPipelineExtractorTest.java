package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kata.easy.Easy29_OptionalPipelineExtractor.Company;
import dev.kata.easy.Easy29_OptionalPipelineExtractor.ContactInfo;
import dev.kata.easy.Easy29_OptionalPipelineExtractor.Department;
import dev.kata.easy.Easy29_OptionalPipelineExtractor.Manager;
import dev.kata.solutions.easy.Easy29_OptionalPipelineExtractorSolution;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 29: Optional nested email extractor")
class Easy29_OptionalPipelineExtractorTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy29_OptionalPipelineExtractor newSolution() {
    return USE_SOLUTION
        ? new Easy29_OptionalPipelineExtractorSolution()
        : new Easy29_OptionalPipelineExtractor();
  }

  private static Company companyWithEmail(String email) {
    return new Company(
        "Acme", new Department("Engineering", new Manager("Alice", new ContactInfo(email))));
  }

  @Test
  @DisplayName("Извлекает валидный email и обрезает пробелы")
  void extractsValidEmail() {
    Optional<String> email = newSolution().extractEmail(companyWithEmail("  Alice@Example.COM  "));

    assertThat(email).contains("Alice@Example.COM");
  }

  @Test
  @DisplayName("null на любом уровне даёт Optional.empty")
  void missingLevels() {
    assertThat(newSolution().extractEmail(null)).isEmpty();
    assertThat(newSolution().extractEmail(new Company("Acme", null))).isEmpty();
    assertThat(newSolution().extractEmail(new Company("Acme", new Department("Eng", null))))
        .isEmpty();
    assertThat(
            newSolution()
                .extractEmail(
                    new Company("Acme", new Department("Eng", new Manager("Alice", null)))))
        .isEmpty();
    assertThat(
            newSolution()
                .extractEmail(
                    new Company(
                        "Acme",
                        new Department("Eng", new Manager("Alice", new ContactInfo(null))))))
        .isEmpty();
  }

  @Test
  @DisplayName("Blank и невалидные email отфильтровываются")
  void invalidEmails() {
    for (String invalid :
        new String[] {
          "",
          "   ",
          "alice",
          "@example.com",
          "alice@",
          "alice@example",
          "alice@.com",
          "alice@example.",
          "a@@example.com",
          "ali ce@example.com"
        }) {
      assertThat(newSolution().extractEmail(companyWithEmail(invalid)))
          .as("email = '%s'", invalid)
          .isEmpty();
    }
  }

  @Test
  @DisplayName("Допускается точка и поддомены в домене")
  void validComplexDomain() {
    assertThat(newSolution().extractEmail(companyWithEmail("a.b@mail.example.co.uk")))
        .contains("a.b@mail.example.co.uk");
  }
}
