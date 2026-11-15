package dev.kata.solutions.prod;

import dev.kata.prod.Kata31_ResilientDataMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Эталонное решение Kata 31.
 *
 * <h2>Почему applicative, а не exceptions</h2>
 *
 * <p>Пять независимых проверок объединяются через {@link Validation#combine}. Каждая проверка
 * возвращает либо нормализованное значение, либо одну ошибку. Комбинация двух invalid складывает
 * списки ошибок, поэтому пользователь получает сразу все проблемы формы. Если бы проверки бросали
 * исключения, пришлось бы ловить их по одной и терять остальные. Исключения остаются только для
 * нарушения контракта: {@code null} вместо DTO, {@code null}-функция, {@code value()} у invalid.
 *
 * <h2>Порядок ошибок</h2>
 *
 * <p>Цепочка {@code id.combine(email).combine(age).combine(country).combine(nickname)} левая:
 * ошибка левой части всегда идёт перед ошибкой правой. Поэтому порядок стабилен — id, email, age,
 * country, nickname — независимо от того, какие поля сломаны.
 *
 * <h2>Две формы</h2>
 *
 * <p>{@link Valid} — record с одним значением; {@link Invalid} — record со списком ошибок. {@code
 * map} и {@code flatMap} на invalid возвращают invalid, не вызывая функцию. {@code combine} на
 * invalid приклеивает ошибки второй стороны, даже если она valid, — так ошибки не теряются.
 *
 * <h2>Нормализация</h2>
 *
 * <p>Trim и регистр применяются до проверки формата: {@code " Alice@Example.COM "} становится
 * {@code "alice@example.com"}, {@code " de "} — {@code "DE"}. {@code nickname == null} — это не
 * ошибка, а отсутствие никнейма; в {@link User} он превращается в пустую строку.
 *
 * <h2>Сложность</h2>
 *
 * <p>Константное число проверок и аллокаций; память O(1), максимум пять ошибок.
 */
public final class Kata31_ResilientDataMapperSolution extends Kata31_ResilientDataMapper {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final int MAX_ID_LENGTH = 36;
  private static final int MIN_AGE = 18;
  private static final int MAX_AGE = 120;
  private static final int MAX_NICKNAME_LENGTH = 30;

  @Override
  public Validation<ValidationError, User> mapUser(RawUser raw) {
    Objects.requireNonNull(raw, "raw");

    Validation<ValidationError, String> id = validateId(raw.id());
    Validation<ValidationError, String> email = validateEmail(raw.email());
    Validation<ValidationError, Integer> age = validateAge(raw.age());
    Validation<ValidationError, String> country = validateCountry(raw.country());
    Validation<ValidationError, String> nickname = validateNickname(raw.nickname());

    return id.combine(email, IdEmail::new)
        .combine(age, (partial, value) -> new IdEmailAge(partial.id(), partial.email(), value))
        .combine(
            country,
            (partial, value) ->
                new IdEmailAgeCountry(partial.id(), partial.email(), partial.age(), value))
        .combine(
            nickname,
            (partial, value) ->
                new User(
                    partial.id(), partial.email(), partial.age(), partial.countryCode(), value));
  }

  private static Validation<ValidationError, String> validateId(String raw) {
    if (raw == null || raw.isBlank()) {
      return invalid("id", "id is required");
    }
    String value = raw.trim();
    if (value.length() > MAX_ID_LENGTH) {
      return invalid("id", "id must be at most " + MAX_ID_LENGTH + " characters");
    }
    return new Valid<>(value);
  }

  private static Validation<ValidationError, String> validateEmail(String raw) {
    if (raw == null || raw.isBlank()) {
      return invalid("email", "email is required");
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (!EMAIL_PATTERN.matcher(value).matches()) {
      return invalid("email", "email must be a valid address");
    }
    return new Valid<>(value);
  }

  private static Validation<ValidationError, Integer> validateAge(Integer raw) {
    if (raw == null) {
      return invalid("age", "age is required");
    }
    if (raw < MIN_AGE || raw > MAX_AGE) {
      return invalid("age", "age must be between " + MIN_AGE + " and " + MAX_AGE);
    }
    return new Valid<>(raw);
  }

  private static Validation<ValidationError, String> validateCountry(String raw) {
    if (raw == null || raw.isBlank()) {
      return invalid("country", "country is required");
    }
    String value = raw.trim().toUpperCase(Locale.ROOT);
    if (value.length() != 2 || !isAsciiLetter(value.charAt(0)) || !isAsciiLetter(value.charAt(1))) {
      return invalid("country", "country must be a 2-letter ISO code");
    }
    return new Valid<>(value);
  }

  private static Validation<ValidationError, String> validateNickname(String raw) {
    if (raw == null) {
      return new Valid<>("");
    }
    String value = raw.trim();
    if (value.length() > MAX_NICKNAME_LENGTH) {
      return invalid("nickname", "nickname must be at most " + MAX_NICKNAME_LENGTH + " characters");
    }
    return new Valid<>(value);
  }

  private static <T> Validation<ValidationError, T> invalid(String field, String message) {
    return new Invalid<>(List.of(new ValidationError(field, message)));
  }

  private static boolean isAsciiLetter(char value) {
    return value >= 'A' && value <= 'Z';
  }

  private record IdEmail(String id, String email) {}

  private record IdEmailAge(String id, String email, int age) {}

  private record IdEmailAgeCountry(String id, String email, int age, String countryCode) {}

  private record Valid<E, T>(T value) implements Validation<E, T> {
    private Valid {
      Objects.requireNonNull(value, "value");
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public T value() {
      return value;
    }

    @Override
    public List<E> errors() {
      return List.of();
    }

    @Override
    public <R> Validation<E, R> map(Function<? super T, ? extends R> mapper) {
      Objects.requireNonNull(mapper, "mapper");
      return new Valid<>(mapper.apply(value));
    }

    @Override
    public <R> Validation<E, R> flatMap(Function<? super T, ? extends Validation<E, R>> mapper) {
      Objects.requireNonNull(mapper, "mapper");
      Validation<E, R> result = mapper.apply(value);
      return Objects.requireNonNull(result, "mapper returned null");
    }

    @Override
    public <U, R> Validation<E, R> combine(
        Validation<E, U> other, BiFunction<? super T, ? super U, ? extends R> combiner) {
      Objects.requireNonNull(other, "other");
      Objects.requireNonNull(combiner, "combiner");
      return other.map(otherValue -> combiner.apply(value, otherValue));
    }
  }

  private record Invalid<E, T>(List<E> errors) implements Validation<E, T> {
    private Invalid {
      Objects.requireNonNull(errors, "errors");
      if (errors.isEmpty()) {
        throw new IllegalArgumentException("invalid validation must have at least one error");
      }
      errors = List.copyOf(errors);
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public T value() {
      throw new NoSuchElementException("Invalid validation: " + errors);
    }

    @Override
    public List<E> errors() {
      return errors;
    }

    @Override
    public <R> Validation<E, R> map(Function<? super T, ? extends R> mapper) {
      Objects.requireNonNull(mapper, "mapper");
      return new Invalid<>(errors);
    }

    @Override
    public <R> Validation<E, R> flatMap(Function<? super T, ? extends Validation<E, R>> mapper) {
      Objects.requireNonNull(mapper, "mapper");
      return new Invalid<>(errors);
    }

    @Override
    public <U, R> Validation<E, R> combine(
        Validation<E, U> other, BiFunction<? super T, ? super U, ? extends R> combiner) {
      Objects.requireNonNull(other, "other");
      Objects.requireNonNull(combiner, "combiner");
      List<E> combined = new ArrayList<>(errors);
      combined.addAll(other.errors());
      return new Invalid<>(combined);
    }
  }
}
