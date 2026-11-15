package dev.kata.solutions.internals;

import dev.kata.internals.Kata07_MiniDependencyInjector;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Эталонное решение Kata 07.
 *
 * <h2>Алгоритм разрешения</h2>
 *
 * <p>Это обычный DFS по графу зависимостей:
 *
 * <ol>
 *   <li>проверяем, не находится ли тип уже в стеке разрешения (цикл);
 *   <li>проверяем регистрацию и наличие {@link Component};
 *   <li>для singleton — смотрим кэш;
 *   <li>выбираем конструктор, рекурсивно разрешаем параметры, создаём экземпляр;
 *   <li>для singleton — кладём в кэш;
 *   <li>в {@code finally} снимаем тип со стека — стек всегда отражает путь текущей ветки DFS, а не
 *       весь граф.
 * </ol>
 *
 * <h2>Почему синхронизация целиком</h2>
 *
 * <p>Разрешение графа — рекурсивный процесс с побочным эффектом (создание singleton'ов). Если
 * синхронизировать только кэш, два потока могут одновременно пройти проверку «singleton ещё не
 * создан», оба создать экземпляр, и один из них будет потерян — а вызывающие получат разные
 * объекты. Поэтому весь {@code get} выполняется под одним монитором. Это не масштабируется на
 * тысячи параллельных запросов, но DI-контейнер обычно работает на старте приложения; для горячего
 * пути singleton'ы кэшируются.
 *
 * <p>{@code ConcurrentHashMap.computeIfAbsent} здесь не подходит: рекурсивное обновление той же
 * карты из вычислительной функции приводит к {@link IllegalStateException} или дедлоку.
 *
 * <h2>MethodHandles вместо Constructor.newInstance</h2>
 *
 * <p>{@link MethodHandles#privateLookupIn(Class, MethodHandles.Lookup)} даёт lookup с приватным
 * доступом к целевому классу, а {@code unreflectConstructor} превращает конструктор в {@link
 * MethodHandle}. Вызов handle после этого проходит без повторных проверок доступа reflection-слоя.
 * Для classpath-приложений (unnamed module) это работает и для package-private/private
 * конструкторов.
 *
 * <h2>Диагностика</h2>
 *
 * <p>Сообщение о цикле строится по стеку разрешения: {@code "A -> B -> C -> A"}. Полные имена
 * классов делают сообщение однозначным, даже если в разных пакетах есть одноимённые классы. Ошибки
 * конструктора оборачиваются с сохранением {@code cause} — иначе причина («нет соединения с БД»)
 * теряется за общим «failed to instantiate».
 */
public final class Kata07_MiniDependencyInjectorSolution extends Kata07_MiniDependencyInjector {

  private final Object lock = new Object();
  private final Map<Class<?>, Object> singletons = new HashMap<>();
  private final Deque<Class<?>> resolutionStack = new ArrayDeque<>();

  public Kata07_MiniDependencyInjectorSolution(Class<?>... componentTypes) {
    super(componentTypes);
  }

  @Override
  public <T> T get(Class<T> type) {
    Objects.requireNonNull(type, "type");
    synchronized (lock) {
      return type.cast(resolve(type));
    }
  }

  /** Разрешает тип. Вызывается только под {@link #lock}. */
  private Object resolve(Class<?> type) {
    if (resolutionStack.stream().anyMatch(inProgress -> inProgress == type)) {
      throw cycleDetected(type);
    }

    if (!componentTypes.contains(type)) {
      throw new DependencyResolutionException(
          "Type " + type.getName() + " is not registered in the container");
    }

    Component component = type.getAnnotation(Component.class);
    if (component == null) {
      throw new DependencyResolutionException(
          "Type " + type.getName() + " is not annotated with @Component");
    }

    if (component.scope() == Scope.SINGLETON) {
      Object cached = singletons.get(type);
      if (cached != null) {
        return cached;
      }
    }

    Constructor<?> constructor = selectConstructor(type);
    Class<?>[] parameterTypes = constructor.getParameterTypes();
    Object[] arguments = new Object[parameterTypes.length];

    resolutionStack.push(type);
    try {
      for (int i = 0; i < parameterTypes.length; i++) {
        if (parameterTypes[i].isPrimitive()) {
          throw new DependencyResolutionException(
              "Cannot inject primitive "
                  + parameterTypes[i].getName()
                  + " into "
                  + type.getName()
                  + ": register a component instead");
        }
        arguments[i] = resolve(parameterTypes[i]);
      }

      Object instance = instantiate(type, constructor, arguments);
      if (component.scope() == Scope.SINGLETON) {
        singletons.put(type, instance);
      }
      return instance;
    } finally {
      // Снимаем тип в любом случае — и при успехе, и при исключении.
      resolutionStack.pop();
    }
  }

  private Constructor<?> selectConstructor(Class<?> type) {
    List<Constructor<?>> injectConstructors = new ArrayList<>();
    for (Constructor<?> candidate : type.getDeclaredConstructors()) {
      if (candidate.isAnnotationPresent(Inject.class)) {
        injectConstructors.add(candidate);
      }
    }

    if (injectConstructors.size() > 1) {
      throw new DependencyResolutionException(
          "Type "
              + type.getName()
              + " has "
              + injectConstructors.size()
              + " constructors annotated with @Inject; exactly one is allowed");
    }
    if (injectConstructors.size() == 1) {
      return injectConstructors.get(0);
    }

    Constructor<?>[] declared = type.getDeclaredConstructors();
    if (declared.length == 1) {
      // Удобство: единственный конструктор не обязан быть помечен @Inject
      // (так работают record'ы и простые value-классы).
      return declared[0];
    }

    throw new DependencyResolutionException(
        "Type "
            + type.getName()
            + " has "
            + declared.length
            + " constructors and none is annotated with @Inject; cannot choose one");
  }

  private Object instantiate(Class<?> type, Constructor<?> constructor, Object[] arguments) {
    try {
      MethodHandle handle =
          MethodHandles.privateLookupIn(type, MethodHandles.lookup())
              .unreflectConstructor(constructor);
      return handle.invokeWithArguments(arguments);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause() == null ? e : e.getCause();
      throw new DependencyResolutionException(
          "Constructor of " + type.getName() + " threw an exception: " + cause, cause);
    } catch (Throwable t) {
      // MethodHandle.invokeWithArguments может пробросить исключение конструктора
      // напрямую, без InvocationTargetException.
      throw new DependencyResolutionException(
          "Failed to instantiate " + type.getName() + ": " + t, t);
    }
  }

  private DependencyResolutionException cycleDetected(Class<?> repeated) {
    StringBuilder path = new StringBuilder();
    // Стек хранит [последний ... первый]; для читаемого пути печатаем от корня.
    var iterator = resolutionStack.descendingIterator();
    while (iterator.hasNext()) {
      path.append(iterator.next().getName()).append(" -> ");
    }
    path.append(repeated.getName());
    return new DependencyResolutionException("Cyclic dependency detected: " + path);
  }
}
