package dev.kata.prod;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Kata 31. Устойчивый маппер «грязного» DTO в доменный объект с накоплением ошибок.
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Импорт пользователей из внешнего CRM приносит «грязные» данные: пробелы, произвольный регистр
 * email и страны, пропущенные поля, возраст вне диапазона. Форма должна показать пользователю сразу
 * все ошибки, а не первую. При этом часть проверок зависит от предыдущих: например, домен email
 * проверяется только после того, как email признан валидным. Для этого нужны две операции:
 *
 * <ul>
 *   <li><b>Applicative {@code combine}</b> — независимые проверки выполняются все, ошибки
 *       накапливаются; значение строится, только если валидны обе стороны.
 *   <li><b>Monadic {@code flatMap}</b> — зависимая проверка выполняется только для валидного
 *       значения; ошибка короткозамыкает цепочку.
 * </ul>
 *
 * <h2>Модель</h2>
 *
 * <p>{@link Validation} — минимальный функциональный тип с двумя формами: {@code Valid(value)} и
 * {@code Invalid(errors)}. {@link #mapUser(RawUser)} нормализует и проверяет пять полей:
 *
 * <ol>
 *   <li>{@code id} — обязателен, после trim не длиннее 36 символов;
 *   <li>{@code email} — обязателен, после trim/нижнего регистра соответствует простому шаблону;
 *   <li>{@code age} — обязателен, целое в диапазоне [18, 120];
 *   <li>{@code country} — обязателен, две латинские буквы, нормализуется в верхний регистр;
 *   <li>{@code nickname} — необязателен, trim, не длиннее 30 символов; отсутствие даёт пустую
 *       строку.
 * </ol>
 *
 * <p>Ошибки всех полей возвращаются в порядке проверки: id, email, age, country, nickname. Наружу
 * отдаются неизменяемые списки; {@link Validation#value()} для invalid бросает {@link
 * java.util.NoSuchElementException}.
 *
 * <h2>Сложность</h2>
 *
 * <p>O(1) по числу полей; памяти O(число ошибок), максимум пять. Никаких исключений для ожидаемых
 * ошибок валидации: это значения, а не исключительные ситуации.
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>{@code null} вместо {@link RawUser} — {@link NullPointerException}: это ошибка
 *       программиста, а не пользовательских данных.
 *   <li>Все пять полей невалидны — пять ошибок, ни одна не потеряна.
 *   <li>Возраст 18 и 120 валиден; 17 и 121 — нет.
 *   <li>Никнейм {@code null} или из пробелов нормализуется в пустую строку.
 *   <li>{@code flatMap} для invalid не вызывает функцию; {@code combine} вызывает комбинатор только
 *       когда валидны оба значения.
 * </ul>
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.prod.Kata31_ResilientDataMapperSolution}. Тесты:
 * {@code Kata31_ResilientDataMapperTest}.
 */
public class Kata31_ResilientDataMapper {

  /** Ошибка одного поля. */
  public record ValidationError(String field, String message) {
    public ValidationError {
      requireText(field, "field");
      requireText(message, "message");
    }
  }

  /** Сырые данные из внешней системы. Любое поле может быть {@code null} или грязным. */
  public record RawUser(String id, String email, Integer age, String country, String nickname) {}

  /** Нормализованный доменный пользователь. Гарантирует инварианты после {@link #mapUser}. */
  public record User(String id, String email, int age, String countryCode, String nickname) {
    public User {
      requireText(id, "id");
      requireText(email, "email");
      if (age < 18 || age > 120) {
        throw new IllegalArgumentException("age must be between 18 and 120, got " + age);
      }
      requireText(countryCode, "countryCode");
      if (countryCode.length() != 2) {
        throw new IllegalArgumentException("countryCode must have 2 letters, got " + countryCode);
      }
      Objects.requireNonNull(nickname, "nickname");
    }
  }

  /**
   * Результат проверки: значение или накопленные ошибки.
   *
   * <p>Инварианты: у valid список ошибок пуст, у invalid он непуст; {@code map}/{@code flatMap} на
   * invalid не вызывают переданную функцию.
   */
  public interface Validation<E, T> {

    boolean isValid();

    /**
     * @throws java.util.NoSuchElementException если проверка не прошла
     */
    T value();

    /** Неизменяемый список ошибок; для valid — пустой. */
    List<E> errors();

    <R> Validation<E, R> map(Function<? super T, ? extends R> mapper);

    <R> Validation<E, R> flatMap(Function<? super T, ? extends Validation<E, R>> mapper);

    /**
     * Applicative-комбинация: ошибки обеих сторон накапливаются, значение строится только при
     * успехе обеих.
     */
    <U, R> Validation<E, R> combine(
        Validation<E, U> other, BiFunction<? super T, ? super U, ? extends R> combiner);
  }

  /**
   * Нормализует и проверяет сырые данные, собирая ошибки всех полей.
   *
   * @param raw сырые данные; не {@code null}
   * @return valid с {@link User} либо invalid со списком {@link ValidationError} в порядке полей
   */
  public Validation<ValidationError, User> mapUser(RawUser raw) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
