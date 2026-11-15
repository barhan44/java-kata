package dev.kata.solutions.internals;

import dev.kata.internals.Kata19_MethodHandleInterceptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Эталонное решение Kata 19.
 *
 * <h2>Кэш MethodHandle</h2>
 *
 * <p>У каждого прокси есть свой {@link ConcurrentHashMap}{@code <Method, TargetInvoker>}. Первый
 * вызов метода создаёт инвокер: {@code privateLookupIn(targetClass)} → {@code
 * unreflect(targetMethod)} → {@code bindTo(target)} → {@code asSpreader(Object[].class, n)} →
 * {@code asType((Object[])Object)}. Повторные вызовы достают готовый handle из карты. {@code
 * computeIfAbsent} потокобезопасен; повторное создание инвокера при гонке безвредно (рефлексия без
 * побочных эффектов).
 *
 * <p>Почему {@code privateLookupIn}, а не {@code MethodHandles.lookup()}? Целевой класс может быть
 * package-private в другом пакете (типичный тестовый/внутренний класс). {@code privateLookupIn}
 * получает lookup с правами целевого класса, а {@code unreflect} находит даже непубличный класс. В
 * named-модулях для этого пакет должен быть открыт, но проект живёт в unnamed module, где это
 * разрешено.
 *
 * <h2>Адаптация сигнатуры</h2>
 *
 * <p>После {@code bindTo(target)} handle принимает аргументы метода. {@code asSpreader}
 * разворачивает {@code Object[]} в отдельные параметры, попутно приводя типы и распаковывая
 * примитивы; {@code asType} приводит возврат к {@code Object} (боксинг примитива) или к {@code
 * void}. Для метода без аргументов используется {@code dropArguments}, потому что {@code
 * asSpreader} с нулём параметров не нужен: проще добавить фиктивный параметр {@code Object[]}. В
 * итоге остаётся {@code invokeExact(args)} — без рефлексии и без упаковки аргументов вручную.
 *
 * <h2>default-методы</h2>
 *
 * <p>{@code Class.getMethod} возвращает наиболее конкретную публичную реализацию. Если целевой
 * класс переопределил default-метод, вернётся метод класса — его и вызывает MethodHandle. Если не
 * переопределил, вернётся метод интерфейса; тогда используется {@link
 * InvocationHandler#invokeDefault(Object, Method, Object[])}: он корректно вызывает default-
 * реализацию на прокси, не требуя target-объекта. Для package-private интерфейсов {@code
 * invokeDefault} из другого пакета бросает {@link IllegalAccessException}; в этом случае включается
 * fallback через {@code privateLookupIn} + {@code unreflectSpecial}, вызывающий default-реализацию
 * на target-объекте.
 *
 * <h2>Цепочка</h2>
 *
 * <p>{@link ChainedInvocation} неизменяем: {@code proceed()} создаёт следующий уровень. Поэтому
 * интерсептор может вызвать {@code proceed()} несколько раз (retry), и каждый раз получит полный
 * остаток цепочки. На каждый уровень — один небольшой объект; это цена гибкости, а не рефлексии.
 * Если интерсептор не вызывает {@code proceed()}, целевой метод не выполняется.
 *
 * <h2>Object-методы</h2>
 *
 * <p>{@code equals}/{@code hashCode}/{@code toString} обрабатываются до цепочки. {@code equals}
 * сравнивает идентичность прокси, {@code hashCode} — identity hash, {@code toString} показывает
 * интерфейс и цель. Это сохраняет контракт {@link Object} и не даёт рекурсии, если интерсептор
 * логирует invocation.
 */
public final class Kata19_MethodHandleInterceptorSolution extends Kata19_MethodHandleInterceptor {

  private static final Object[] NO_ARGS = new Object[0];

  /**
   * Создаёт прокси. Цепочка интерсепторов копируется, поэтому последующее изменение исходного
   * массива на прокси не влияет.
   */
  public static <T> T createProxy(
      Class<T> interfaceType, T target, MethodInterceptor... interceptors) {
    Objects.requireNonNull(interfaceType, "interfaceType");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(interceptors, "interceptors");
    if (!interfaceType.isInterface()) {
      throw new IllegalArgumentException(
          "interfaceType must be an interface, got " + interfaceType);
    }
    if (!interfaceType.isInstance(target)) {
      throw new IllegalArgumentException(
          "target must implement " + interfaceType.getName() + ", got " + target.getClass());
    }

    MethodInterceptor[] chain = interceptors.clone();
    for (int i = 0; i < chain.length; i++) {
      Objects.requireNonNull(chain[i], "interceptors[" + i + "]");
    }

    InvocationHandler handler = new InterceptorInvocationHandler(interfaceType, target, chain);
    Object proxy =
        Proxy.newProxyInstance(
            interfaceType.getClassLoader(), new Class<?>[] {interfaceType}, handler);
    return interfaceType.cast(proxy);
  }

  /** Инвокер целевого метода, адаптированный к {@code (Object[])Object}. */
  @FunctionalInterface
  private interface TargetInvoker {
    Object invoke(Object proxy, Object[] args) throws Throwable;
  }

  private static final class InterceptorInvocationHandler implements InvocationHandler {

    private final Class<?> interfaceType;
    private final Object target;
    private final MethodInterceptor[] interceptors;
    private final ConcurrentHashMap<Method, TargetInvoker> invokers = new ConcurrentHashMap<>();

    InterceptorInvocationHandler(
        Class<?> interfaceType, Object target, MethodInterceptor[] interceptors) {
      this.interfaceType = interfaceType;
      this.target = target;
      this.interceptors = interceptors;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getDeclaringClass() == Object.class) {
        return invokeObjectMethod(proxy, method, args);
      }
      Object[] arguments = args == null ? NO_ARGS : args;
      return new ChainedInvocation(0, proxy, method, arguments).proceed();
    }

    private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
      return switch (method.getName()) {
        case "equals" -> proxy == args[0];
        case "hashCode" -> System.identityHashCode(proxy);
        case "toString" -> "Proxy[" + interfaceType.getSimpleName() + "]->" + target;
        default -> throw new IllegalStateException("unexpected Object method: " + method);
      };
    }

    private TargetInvoker createInvoker(Method method) {
      Method targetMethod = findTargetMethod(method);

      if (targetMethod == null) {
        if (method.isDefault()) {
          return defaultInvoker(method);
        }
        throw new IllegalArgumentException(
            "target " + target.getClass().getName() + " does not implement " + method);
      }

      if (method.isDefault() && targetMethod.getDeclaringClass().isInterface()) {
        // Цель не переопределяет default-метод: вызываем реализацию интерфейса.
        return defaultInvoker(method);
      }

      MethodHandle handle;
      try {
        MethodHandles.Lookup lookup =
            MethodHandles.privateLookupIn(target.getClass(), MethodHandles.lookup());
        handle = lookup.unreflect(targetMethod).bindTo(target);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("cannot access target method " + targetMethod, e);
      }
      return adapt(handle, method);
    }

    /**
     * Инвокер default-метода: сначала штатный {@link InvocationHandler#invokeDefault}, а если
     * интерфейс package-private и handler из другого пакета не имеет доступа — MethodHandle,
     * полученный через {@code privateLookupIn} и {@code unreflectSpecial}.
     *
     * <p>Fallback вызывает default-реализацию на целевом объекте, а не на прокси. Для
     * package-private интерфейсов это единственный доступный путь; вызовы других методов внутри
     * default-реализации пойдут напрямую в цель, минуя цепочку интерсепторов. Для публичных
     * интерфейсов используется штатный {@code invokeDefault}, сохраняющий поведение прокси.
     */
    private TargetInvoker defaultInvoker(Method method) {
      TargetInvoker proxyDefault =
          (proxy, args) -> InvocationHandler.invokeDefault(proxy, method, args);

      MethodHandle fallbackHandle;
      try {
        MethodHandles.Lookup lookup =
            MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup());
        fallbackHandle = lookup.unreflectSpecial(method, method.getDeclaringClass()).bindTo(target);
      } catch (IllegalAccessException | RuntimeException cannotBuildFallback) {
        return proxyDefault;
      }

      TargetInvoker fallback = adapt(fallbackHandle, method);
      return (proxy, args) -> {
        try {
          return proxyDefault.invoke(proxy, args);
        } catch (IllegalAccessException accessDenied) {
          return fallback.invoke(proxy, args);
        }
      };
    }

    /** Адаптирует handle с уже привязанным receiver к {@code (Object[])Object}. */
    private TargetInvoker adapt(MethodHandle bound, Method method) {
      int argumentCount = method.getParameterCount();
      MethodHandle spreader =
          argumentCount == 0
              ? MethodHandles.dropArguments(bound, 0, Object[].class)
              : bound.asSpreader(Object[].class, argumentCount);

      if (method.getReturnType() == void.class) {
        MethodHandle voidHandle =
            spreader.asType(MethodType.methodType(void.class, Object[].class));
        return (proxy, args) -> {
          voidHandle.invokeExact(args);
          return null;
        };
      }

      MethodHandle objectHandle =
          spreader.asType(MethodType.methodType(Object.class, Object[].class));
      return (proxy, args) -> objectHandle.invokeExact(args);
    }

    private Method findTargetMethod(Method method) {
      try {
        return target.getClass().getMethod(method.getName(), method.getParameterTypes());
      } catch (NoSuchMethodException e) {
        return null;
      }
    }

    /** Один уровень цепочки. Неизменяем, поэтому proceed() можно вызывать повторно. */
    private final class ChainedInvocation implements MethodInvocation {
      private final int index;
      private final Object proxy;
      private final Method method;
      private final Object[] arguments;

      ChainedInvocation(int index, Object proxy, Method method, Object[] arguments) {
        this.index = index;
        this.proxy = proxy;
        this.method = method;
        this.arguments = arguments;
      }

      @Override
      public Object proceed() throws Throwable {
        if (index < interceptors.length) {
          return interceptors[index].intercept(
              new ChainedInvocation(index + 1, proxy, method, arguments));
        }
        TargetInvoker invoker =
            invokers.computeIfAbsent(method, InterceptorInvocationHandler.this::createInvoker);
        return invoker.invoke(proxy, arguments);
      }

      @Override
      public Method method() {
        return method;
      }

      @Override
      public Object target() {
        return target;
      }

      @Override
      public Object[] arguments() {
        return arguments;
      }
    }
  }
}
