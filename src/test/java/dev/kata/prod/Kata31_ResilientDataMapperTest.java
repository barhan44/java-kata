package dev.kata.prod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.prod.Kata31_ResilientDataMapper.RawUser;
import dev.kata.prod.Kata31_ResilientDataMapper.User;
import dev.kata.prod.Kata31_ResilientDataMapper.Validation;
import dev.kata.prod.Kata31_ResilientDataMapper.ValidationError;
import dev.kata.solutions.prod.Kata31_ResilientDataMapperSolution;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 31.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata31* -DuseSolution=true</pre>
 */
@DisplayName("Kata 31: resilient data mapper")
class Kata31_ResilientDataMapperTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata31_ResilientDataMapper newMapper() {
    return USE_SOLUTION
        ? new Kata31_ResilientDataMapperSolution()
        : new Kata31_ResilientDataMapper();
  }

  @Test
  @DisplayName("Валидные грязные данные нормализуются")
  void mapsAndNormalizesValidData() {
    Validation<ValidationError, User> result =
        newMapper()
            .mapUser(new RawUser("  u-1  ", "  Alice@Example.COM ", 30, " de ", "  Alice  "));

    assertThat(result.isValid()).isTrue();
    assertThat(result.errors()).isEmpty();
    assertThat(result.value()).isEqualTo(new User("u-1", "alice@example.com", 30, "DE", "Alice"));
  }

  @Test
  @DisplayName("Три невалидных поля дают ровно три ошибки в порядке полей")
  void accumulatesThreeErrors() {
    Validation<ValidationError, User> result =
        newMapper().mapUser(new RawUser(" ", "not-an-email", 15, "DE", null));

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors())
        .extracting(ValidationError::field)
        .containsExactly("id", "email", "age");
    assertThat(result.errors())
        .extracting(ValidationError::message)
        .containsExactly(
            "id is required", "email must be a valid address", "age must be between 18 and 120");
    assertThatThrownBy(() -> result.value())
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("id");
  }

  @Test
  @DisplayName("Все пять полей невалидны — ни одна ошибка не потеряна")
  void accumulatesAllFiveErrors() {
    Validation<ValidationError, User> result =
        newMapper().mapUser(new RawUser("", "bad", null, "X", "n".repeat(31)));

    assertThat(result.errors())
        .extracting(ValidationError::field)
        .containsExactly("id", "email", "age", "country", "nickname");
    assertThat(result.errors())
        .extracting(ValidationError::message)
        .containsExactly(
            "id is required",
            "email must be a valid address",
            "age is required",
            "country must be a 2-letter ISO code",
            "nickname must be at most 30 characters");
  }

  @Test
  @DisplayName("Границы возраста 18 и 120 включительны")
  void ageBoundaries() {
    Kata31_ResilientDataMapper mapper = newMapper();

    assertThat(mapper.mapUser(userWithAge(18)).isValid()).isTrue();
    assertThat(mapper.mapUser(userWithAge(120)).isValid()).isTrue();
    assertThat(mapper.mapUser(userWithAge(17)).errors())
        .extracting(ValidationError::field)
        .containsExactly("age");
    assertThat(mapper.mapUser(userWithAge(121)).errors())
        .extracting(ValidationError::field)
        .containsExactly("age");
  }

  @Test
  @DisplayName("Email: обязателен, нормализуется, проверяется шаблоном")
  void emailRules() {
    Kata31_ResilientDataMapper mapper = newMapper();

    assertThat(errorsForEmail(null))
        .extracting(ValidationError::message)
        .containsExactly("email is required");
    assertThat(errorsForEmail("   "))
        .extracting(ValidationError::message)
        .containsExactly("email is required");
    assertThat(errorsForEmail("no-at.example.com"))
        .extracting(ValidationError::message)
        .containsExactly("email must be a valid address");
    assertThat(errorsForEmail("a@b"))
        .extracting(ValidationError::message)
        .containsExactly("email must be a valid address");
    assertThat(errorsForEmail("a b@c.de"))
        .extracting(ValidationError::message)
        .containsExactly("email must be a valid address");

    Validation<ValidationError, User> valid =
        mapper.mapUser(new RawUser("u", "  MIXED@Example.COM ", 20, "DE", null));
    assertThat(valid.value().email()).isEqualTo("mixed@example.com");
  }

  @Test
  @DisplayName("Страна: две латинские буквы, верхний регистр, обязательна")
  void countryRules() {
    Kata31_ResilientDataMapper mapper = newMapper();

    assertThat(errorsForCountry(null))
        .extracting(ValidationError::message)
        .containsExactly("country is required");
    assertThat(errorsForCountry("  "))
        .extracting(ValidationError::message)
        .containsExactly("country is required");
    for (String invalid : List.of("D", "DEU", "1A", "dé", "D1")) {
      assertThat(errorsForCountry(invalid))
          .as("country %s", invalid)
          .extracting(ValidationError::message)
          .containsExactly("country must be a 2-letter ISO code");
    }

    assertThat(mapper.mapUser(new RawUser("u", "a@b.de", 20, " de ", null)).value().countryCode())
        .isEqualTo("DE");
  }

  @Test
  @DisplayName("Никнейм необязателен, обрезается и ограничен 30 символами")
  void nicknameRules() {
    Kata31_ResilientDataMapper mapper = newMapper();

    assertThat(mapper.mapUser(rawWithNickname(null)).value().nickname()).isEmpty();
    assertThat(mapper.mapUser(rawWithNickname("   ")).value().nickname()).isEmpty();
    assertThat(mapper.mapUser(rawWithNickname("  Bob  ")).value().nickname()).isEqualTo("Bob");
    assertThat(mapper.mapUser(rawWithNickname("n".repeat(30))).isValid()).isTrue();
    assertThat(mapper.mapUser(rawWithNickname("n".repeat(31))).errors())
        .extracting(ValidationError::field)
        .containsExactly("nickname");
  }

  @Test
  @DisplayName("Id: обязателен, trim, не длиннее 36 символов")
  void idRules() {
    Kata31_ResilientDataMapper mapper = newMapper();

    assertThat(mapper.mapUser(new RawUser("  id-1 ", "a@b.de", 20, "DE", null)).value().id())
        .isEqualTo("id-1");
    assertThat(mapper.mapUser(new RawUser("x".repeat(36), "a@b.de", 20, "DE", null)).isValid())
        .isTrue();
    assertThat(mapper.mapUser(new RawUser("x".repeat(37), "a@b.de", 20, "DE", null)).errors())
        .extracting(ValidationError::field)
        .containsExactly("id");
  }

  @Test
  @DisplayName("map применяет функцию к valid и не вызывается для invalid")
  void mapRespectsValidity() {
    Kata31_ResilientDataMapper mapper = newMapper();
    Validation<ValidationError, User> valid = mapper.mapUser(validRaw());
    assertThat(valid.map(User::email).value()).isEqualTo("alice@example.com");

    Validation<ValidationError, User> invalid =
        mapper.mapUser(new RawUser("", "bad", 20, "DE", null));
    AtomicBoolean called = new AtomicBoolean();
    Validation<ValidationError, String> mapped =
        invalid.map(
            user -> {
              called.set(true);
              return user.email();
            });

    assertThat(mapped.isValid()).isFalse();
    assertThat(mapped.errors()).isEqualTo(invalid.errors());
    assertThat(called).isFalse();
  }

  @Test
  @DisplayName("flatMap короткозамыкает invalid, не вызывая зависимую проверку")
  void flatMapShortCircuits() {
    Kata31_ResilientDataMapper mapper = newMapper();
    Validation<ValidationError, User> invalid =
        mapper.mapUser(new RawUser(" ", "not-an-email", 15, "DE", null));
    AtomicBoolean called = new AtomicBoolean();

    Validation<ValidationError, User> result =
        invalid.flatMap(
            user -> {
              called.set(true);
              return mapper.mapUser(validRaw());
            });

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors()).isEqualTo(invalid.errors());
    assertThat(called).isFalse();
  }

  @Test
  @DisplayName("flatMap выполняет зависимую проверку для valid")
  void flatMapChainsDependentValidation() {
    Kata31_ResilientDataMapper mapper = newMapper();
    Validation<ValidationError, User> base = mapper.mapUser(validRaw());

    Validation<ValidationError, User> success =
        base.flatMap(
            user ->
                mapper.mapUser(
                    new RawUser(user.id(), user.email(), 19, user.countryCode(), user.nickname())));
    assertThat(success.isValid()).isTrue();
    assertThat(success.value().age()).isEqualTo(19);

    Validation<ValidationError, User> failure =
        base.flatMap(
            user ->
                mapper.mapUser(new RawUser(user.id(), user.email(), 10, user.countryCode(), "")));
    assertThat(failure.isValid()).isFalse();
    assertThat(failure.errors()).extracting(ValidationError::field).containsExactly("age");
  }

  @Test
  @DisplayName("combine объединяет два valid значения")
  void combineValidValues() {
    Kata31_ResilientDataMapper mapper = newMapper();
    Validation<ValidationError, User> first =
        mapper.mapUser(new RawUser("u1", "a@b.de", 20, "DE", null));
    Validation<ValidationError, User> second =
        mapper.mapUser(new RawUser("u2", "b@c.de", 30, "FR", null));

    Validation<ValidationError, String> combined =
        first.combine(second, (left, right) -> left.id() + "+" + right.id());

    assertThat(combined.isValid()).isTrue();
    assertThat(combined.value()).isEqualTo("u1+u2");
  }

  @Test
  @DisplayName("combine накапливает ошибки обеих сторон в стабильном порядке")
  void combineAccumulatesErrors() {
    Kata31_ResilientDataMapper mapper = newMapper();
    Validation<ValidationError, User> invalidA =
        mapper.mapUser(new RawUser("", "bad", 20, "DE", null));
    Validation<ValidationError, User> invalidB =
        mapper.mapUser(new RawUser("u", "b@c.de", 15, "X", null));
    AtomicBoolean called = new AtomicBoolean();

    Validation<ValidationError, String> both =
        invalidA.combine(
            invalidB,
            (left, right) -> {
              called.set(true);
              return left.id() + right.id();
            });

    assertThat(both.isValid()).isFalse();
    assertThat(both.errors())
        .extracting(ValidationError::field)
        .containsExactly("id", "email", "age", "country");
    assertThat(called).isFalse();

    Validation<ValidationError, User> valid = mapper.mapUser(validRaw());
    assertThat(valid.combine(invalidB, (l, r) -> l).errors()).isEqualTo(invalidB.errors());
    assertThat(invalidA.combine(valid, (l, r) -> r).errors()).isEqualTo(invalidA.errors());
  }

  @Test
  @DisplayName("Возвращаемые списки ошибок неизменяемы")
  void errorsAreImmutable() {
    Validation<ValidationError, User> invalid =
        newMapper().mapUser(new RawUser("", "bad", 20, "DE", null));

    assertThatThrownBy(() -> invalid.errors().add(new ValidationError("x", "y")))
        .isInstanceOf(UnsupportedOperationException.class);

    Validation<ValidationError, User> valid = newMapper().mapUser(validRaw());
    assertThatThrownBy(() -> valid.errors().add(new ValidationError("x", "y")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("null-аргументы и null-функции отклоняются")
  void nullArguments() {
    Kata31_ResilientDataMapper mapper = newMapper();
    assertThatThrownBy(() -> mapper.mapUser(null)).isInstanceOf(NullPointerException.class);

    Validation<ValidationError, User> valid = mapper.mapUser(validRaw());
    Validation<ValidationError, User> invalid =
        mapper.mapUser(new RawUser("", "bad", 20, "DE", null));
    Validation<ValidationError, User> other = mapper.mapUser(validRaw());

    assertThatThrownBy(() -> valid.map(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> valid.flatMap(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> invalid.map(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> invalid.flatMap(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> valid.combine(null, (User a, User b) -> a))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> valid.combine(other, null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> invalid.combine((Validation<ValidationError, User>) null, (a, b) -> a))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> invalid.combine(other, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Валидация ValidationError, RawUser и User")
  void recordValidation() {
    assertThatThrownBy(() -> new ValidationError(null, "message"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ValidationError(" ", "message"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ValidationError("field", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ValidationError("field", " "))
        .isInstanceOf(IllegalArgumentException.class);

    // RawUser намеренно не валидирует данные: это задача маппера.
    RawUser dirty = new RawUser(null, null, null, null, null);
    assertThat(dirty.id()).isNull();

    assertThatThrownBy(() -> new User("", "a@b.de", 20, "DE", ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new User("u", "", 20, "DE", ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new User("u", "a@b.de", 17, "DE", ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new User("u", "a@b.de", 121, "DE", ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new User("u", "a@b.de", 20, "DEU", ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new User("u", "a@b.de", 20, "DE", null))
        .isInstanceOf(NullPointerException.class);
  }

  private List<ValidationError> errorsForEmail(String email) {
    return newMapper().mapUser(new RawUser("u", email, 20, "DE", null)).errors();
  }

  private List<ValidationError> errorsForCountry(String country) {
    return newMapper().mapUser(new RawUser("u", "a@b.de", 20, country, null)).errors();
  }

  private RawUser userWithAge(int age) {
    return new RawUser("u", "a@b.de", age, "DE", null);
  }

  private RawUser rawWithNickname(String nickname) {
    return new RawUser("u", "a@b.de", 20, "DE", nickname);
  }

  private static RawUser validRaw() {
    return new RawUser("u-1", "alice@example.com", 30, "DE", "Alice");
  }
}
