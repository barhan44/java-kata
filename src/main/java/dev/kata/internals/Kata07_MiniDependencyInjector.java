package dev.kata.internals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Kata 07. Мини-DI-контейнер на Reflection + MethodHandles.
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Понимать, как устроены Spring, Guice и Dagger, невозможно без собственной наивной реализации.
 * Нужно уметь: читать аннотации, строить граф зависимостей, разрешать его в правильном порядке,
 * кэшировать singleton'ы, создавать prototype-экземпляры и — что важнее всего — <b>понятно
 * падать</b> при циклах и отсутствующих зависимостях. Именно диагностика отличает учебный контейнер
 * от продакшн-фреймворка.
 *
 * <h2>Контракт</h2>
 *
 * <ul>
 *   <li>Компоненты передаются в конструктор контейнера: {@code new
 *       MiniDependencyInjector(Repository.class, Service.class)}. Разрешать можно только
 *       зарегистрированные классы.
 *   <li>Класс компонента помечается {@link Component}; скоуп задаётся атрибутом {@code scope} (по
 *       умолчанию {@link Scope#SINGLETON}).
 *   <li>Конструктор для инъекции помечается {@link Inject}. Если {@code @Inject} нет, но у класса
 *       ровно один конструктор — используется он (удобство для record'ов и простых классов). Если
 *       {@code @Inject} несколько или конструкторов несколько без аннотации — {@link
 *       DependencyResolutionException}.
 *   <li>Аргументы конструктора разрешаются рекурсивно по типам. Примитивы и {@code
 *       null}-зависимости не поддерживаются.
 *   <li>{@link Scope#SINGLETON}: экземпляр создаётся лениво при первом запросе и затем
 *       переиспользуется. {@link Scope#PROTOTYPE}: новый экземпляр на каждый {@code get} (и на
 *       каждую инъекцию в другой компонент).
 *   <li>Цикл зависимостей — {@link DependencyResolutionException} с путём вида {@code "A -> B -> C
 *       -> A"} (используйте полные имена классов, чтобы сообщение было однозначным).
 *   <li>Отсутствующая/незарегистрированная зависимость — {@link DependencyResolutionException}, в
 *       сообщении — имя запрошенного типа.
 *   <li>{@link #get(Class)} потокобезопасен: одновременные запросы одного singleton'а должны
 *       вернуть один и тот же экземпляр, а не создать два.
 * </ul>
 *
 * <h2>Подводные камни</h2>
 *
 * <ul>
 *   <li><b>Циклы.</b> Наивная рекурсия без стека разрешения уйдёт в {@link StackOverflowError}
 *       вместо понятной ошибки. Ведите стек «кто сейчас создаётся» и проверяйте вхождение типа до
 *       рекурсии.
 *   <li><b>Порядок проверок.</b> Если singleton уже создан, его можно возвращать сразу, не проверяя
 *       цикл. Но нельзя кэшировать «наполовину созданный» объект.
 *   <li><b>Исключения конструктора.</b> Если конструктор бросил исключение, контейнер не должен
 *       оставить «битый» singleton в кэше и должен сохранить исходную причину ({@code cause}).
 *   <li><b>Reflection vs MethodHandles.</b> {@code Constructor.newInstance} каждый раз делает
 *       проверки доступа. {@link java.lang.invoke.MethodHandle} позволяет один раз «скомпилировать»
 *       вызов и затем вызывать его дешевле; для приватных конструкторов нужен {@link
 *       java.lang.invoke.MethodHandles#privateLookupIn(Class,
 *       java.lang.invoke.MethodHandles.Lookup)}.
 *   <li><b>Потокобезопасность.</b> Синхронизируйте разрешение графа целиком: иначе два потока могут
 *       одновременно создать два singleton'а, и один из них «потеряется». Проверка-и-вставка в
 *       {@code ConcurrentHashMap} с рекурсией опасна (recursive update exception).
 *   <li><b>Примитивы.</b> {@code int}/{@code long} в конструкторе нельзя разрешить как компонент —
 *       лучше явно сообщить об этом, чем получить {@link NoSuchMethodException} или NPE.
 * </ul>
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>Пустой конструктор зависимостей — компонент без зависимостей.
 *   <li>Один и тот же класс передан дважды — регистрация не должна падать.
 *   <li>{@code get(null)} — {@link NullPointerException}.
 *   <li>Запрос незарегистрированного класса — {@link DependencyResolutionException}.
 *   <li>Цикл из двух и более компонентов — сообщение содержит весь путь.
 * </ul>
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.internals.Kata07_MiniDependencyInjectorSolution}.
 */
public class Kata07_MiniDependencyInjector {

  /** Помечает конструктор, который контейнер должен использовать для инъекции. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.CONSTRUCTOR)
  public @interface Inject {}

  /** Помечает класс как компонент контейнера. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface Component {
    /** Скоуп компонента; по умолчанию — singleton. */
    Scope scope() default Scope.SINGLETON;
  }

  /** Жизненный цикл компонента. */
  public enum Scope {
    /** Один экземпляр на контейнер. */
    SINGLETON,
    /** Новый экземпляр на каждый запрос. */
    PROTOTYPE
  }

  /** Ошибка разрешения графа зависимостей. */
  public static class DependencyResolutionException extends RuntimeException {
    public DependencyResolutionException(String message) {
      super(message);
    }

    public DependencyResolutionException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Зарегистрированные типы компонентов. */
  protected final Set<Class<?>> componentTypes;

  /**
   * @param componentTypes классы компонентов; порядок не важен, дубликаты игнорируются
   */
  public Kata07_MiniDependencyInjector(Class<?>... componentTypes) {
    this.componentTypes = new LinkedHashSet<>(Arrays.asList(componentTypes));
  }

  /**
   * Возвращает экземпляр компонента, разрешая граф зависимостей.
   *
   * @param type зарегистрированный тип компонента
   * @param <T> тип компонента
   * @return экземпляр (общий для singleton, новый для prototype)
   */
  public <T> T get(Class<T> type) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
