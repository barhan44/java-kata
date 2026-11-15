package dev.kata.internals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.internals.Kata19_MethodHandleInterceptor.MethodInterceptor;
import dev.kata.solutions.internals.Kata19_MethodHandleInterceptorSolution;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 19.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata19* -DuseSolution=true</pre>
 */
@DisplayName("Kata 19: AOP-прокси на MethodHandle")
public class Kata19_MethodHandleInterceptorTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  public interface Greeter {
    String greet(String name);

    default String defaultGreeting() {
      return "default";
    }

    int add(int a, int b);

    void record(String value);

    String fail() throws IOException;

    String flaky() throws IOException;

    String noArgs();
  }

  static class TestTarget implements Greeter {
    final List<String> recorded = new CopyOnWriteArrayList<>();
    final IOException failure = new IOException("boom");
    final AtomicInteger greetCalls = new AtomicInteger();
    int flakyCalls;

    @Override
    public String greet(String name) {
      greetCalls.incrementAndGet();
      return "Hello, " + name;
    }

    @Override
    public int add(int a, int b) {
      return a + b;
    }

    @Override
    public void record(String value) {
      recorded.add(value);
    }

    @Override
    public String fail() throws IOException {
      throw failure;
    }

    @Override
    public String flaky() throws IOException {
      if (++flakyCalls == 1) {
        throw new IOException("first attempt");
      }
      return "ok";
    }

    @Override
    public String noArgs() {
      return "no-args";
    }
  }

  static final class OverridingTarget extends TestTarget {
    @Override
    public String defaultGreeting() {
      return "override";
    }
  }

  /** Пакетно-приватный интерфейс: invokeDefault из другого пакета не имеет доступа. */
  interface HiddenGreeter {
    default String hiddenGreeting() {
      return "hidden";
    }
  }

  static final class HiddenTarget implements HiddenGreeter {}

  @Test
  @DisplayName("Обычный метод делегируется цели через цепочку before/after")
  void regularMethodWithBeforeAfterChain() {
    TestTarget target = new TestTarget();
    List<String> events = new CopyOnWriteArrayList<>();

    MethodInterceptor first =
        invocation -> {
          events.add("before-1");
          try {
            return invocation.proceed();
          } finally {
            events.add("after-1");
          }
        };
    MethodInterceptor second =
        invocation -> {
          events.add("before-2");
          try {
            return invocation.proceed();
          } finally {
            events.add("after-2");
          }
        };

    Greeter proxy = newProxy(Greeter.class, target, first, second);

    assertThat(proxy.greet("Alice")).isEqualTo("Hello, Alice");
    assertThat(events).containsExactly("before-1", "before-2", "after-2", "after-1");
    assertThat(target.greetCalls).hasValue(1);
  }

  @Test
  @DisplayName("default-метод без override вызывается через invokeDefault")
  void defaultMethodWithoutOverride() {
    TestTarget target = new TestTarget();
    AtomicInteger intercepted = new AtomicInteger();

    Greeter proxy =
        newProxy(
            Greeter.class,
            target,
            invocation -> {
              intercepted.incrementAndGet();
              return invocation.proceed();
            });

    assertThat(proxy.defaultGreeting()).isEqualTo("default");
    assertThat(intercepted).hasValue(1);
  }

  @Test
  @DisplayName("default-метод, переопределённый целью, вызывает реализацию цели")
  void overriddenDefaultMethodUsesTarget() {
    OverridingTarget target = new OverridingTarget();
    Greeter proxy = newProxy(Greeter.class, target);

    assertThat(proxy.defaultGreeting()).isEqualTo("override");
  }

  @Test
  @DisplayName("default-метод package-private интерфейса работает через MethodHandle fallback")
  void defaultMethodOnPackagePrivateInterface() {
    HiddenGreeter proxy = newProxy(HiddenGreeter.class, new HiddenTarget());

    assertThat(proxy.hiddenGreeting()).isEqualTo("hidden");
  }

  @Test
  @DisplayName("Примитивы, void и метод без аргументов адаптируются корректно")
  void primitiveVoidAndNoArgMethods() {
    TestTarget target = new TestTarget();
    Greeter proxy = newProxy(Greeter.class, target);

    assertThat(proxy.add(2, 3)).isEqualTo(5);
    proxy.record("value");
    assertThat(target.recorded).containsExactly("value");
    assertThat(proxy.noArgs()).isEqualTo("no-args");
  }

  @Test
  @DisplayName("Object-методы обрабатываются специально и не идут через интерсепторы")
  void objectMethodsBypassInterceptors() {
    TestTarget target = new TestTarget();
    AtomicInteger intercepted = new AtomicInteger();
    Greeter proxy =
        newProxy(
            Greeter.class,
            target,
            invocation -> {
              intercepted.incrementAndGet();
              return invocation.proceed();
            });

    assertThat(proxy.greet("x")).isEqualTo("Hello, x");
    assertThat(intercepted).hasValue(1);

    assertThat(proxy.toString()).contains("Greeter").contains("TestTarget");
    assertThat(proxy.hashCode()).isEqualTo(System.identityHashCode(proxy));
    assertThat(proxy.equals(proxy)).isTrue();
    assertThat(proxy.equals(target)).isFalse();
    assertThat(intercepted).hasValue(1);
  }

  @Test
  @DisplayName("Исключение цели пробрасывается без InvocationTargetException")
  void targetExceptionPropagatesUnwrapped() {
    TestTarget target = new TestTarget();
    List<String> events = new CopyOnWriteArrayList<>();

    MethodInterceptor outer =
        invocation -> {
          try {
            return invocation.proceed();
          } catch (IOException e) {
            events.add("outer-error");
            throw e;
          }
        };
    MethodInterceptor inner =
        invocation -> {
          try {
            return invocation.proceed();
          } catch (IOException e) {
            events.add("inner-error");
            throw e;
          }
        };

    Greeter proxy = newProxy(Greeter.class, target, outer, inner);

    assertThatThrownBy(proxy::fail).isSameAs(target.failure);
    assertThat(events).containsExactly("inner-error", "outer-error");
  }

  @Test
  @DisplayName("Retry-интерсептор может вызвать proceed() повторно")
  void retryInterceptor() throws Exception {
    TestTarget target = new TestTarget();
    AtomicInteger attempts = new AtomicInteger();

    MethodInterceptor retry =
        invocation -> {
          attempts.incrementAndGet();
          try {
            return invocation.proceed();
          } catch (IOException firstFailure) {
            return invocation.proceed();
          }
        };

    Greeter proxy = newProxy(Greeter.class, target, retry);

    assertThat(proxy.flaky()).isEqualTo("ok");
    assertThat(attempts).hasValue(1); // тело интерсептора выполнилось один раз, proceed — дважды
    assertThat(target.flakyCalls).isEqualTo(2);
  }

  @Test
  @DisplayName("Интерсептор без proceed() замыкает вызов, цель не выполняется")
  void shortCircuitInterceptor() {
    TestTarget target = new TestTarget();
    Greeter proxy = newProxy(Greeter.class, target, invocation -> "cached");

    assertThat(proxy.greet("Alice")).isEqualTo("cached");
    assertThat(target.greetCalls).hasValue(0);
  }

  @Test
  @DisplayName("MethodInvocation отдаёт метод, цель и аргументы")
  void invocationMetadata() {
    TestTarget target = new TestTarget();

    MethodInterceptor metadata =
        invocation -> {
          Method method = invocation.method();
          assertThat(method.getName()).isEqualTo("greet");
          assertThat(invocation.target()).isSameAs(target);
          assertThat(invocation.arguments()).containsExactly("Alice");
          return invocation.proceed();
        };

    Greeter proxy = newProxy(Greeter.class, target, metadata);
    assertThat(proxy.greet("Alice")).isEqualTo("Hello, Alice");
  }

  @Test
  @DisplayName("Интерсептор может изменить аргументы до proceed()")
  void interceptorCanModifyArguments() {
    TestTarget target = new TestTarget();
    MethodInterceptor rename =
        invocation -> {
          invocation.arguments()[0] = "Bob";
          return invocation.proceed();
        };

    Greeter proxy = newProxy(Greeter.class, target, rename);
    assertThat(proxy.greet("Alice")).isEqualTo("Hello, Bob");
  }

  @Test
  @DisplayName("Прокси без интерсепторов напрямую делегирует цели")
  void zeroInterceptors() {
    TestTarget target = new TestTarget();
    Greeter proxy = newProxy(Greeter.class, target);

    assertThat(proxy.greet("Alice")).isEqualTo("Hello, Alice");
    assertThat(proxy.add(4, 5)).isEqualTo(9);
    assertThat(proxy.defaultGreeting()).isEqualTo("default");
  }

  @Test
  @DisplayName("Ошибка интерсептора пробрасывается как есть")
  void interceptorExceptionPropagates() {
    TestTarget target = new TestTarget();
    RuntimeException failure = new RuntimeException("interceptor failure");
    MethodInterceptor failing =
        invocation -> {
          throw failure;
        };

    Greeter proxy = newProxy(Greeter.class, target, failing);
    assertThatThrownBy(() -> proxy.greet("x")).isSameAs(failure);
  }

  @Test
  @DisplayName("Кэш MethodHandle потокобезопасен при конкурентных вызовах")
  void concurrentCallsUseCachedHandles() throws Exception {
    TestTarget target = new TestTarget();
    Greeter proxy = newProxy(Greeter.class, target);

    int threads = 8;
    int callsPerThread = 2_000;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int thread = 0; thread < threads; thread++) {
        int threadId = thread;
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  for (int i = 0; i < callsPerThread; i++) {
                    if (proxy.add(threadId, i) != threadId + i) {
                      throw new AssertionError("wrong result");
                    }
                  }
                  return null;
                }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("Валидация аргументов createProxy")
  void invalidArguments() {
    TestTarget target = new TestTarget();

    assertThatThrownBy(() -> newProxy(null, target)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newProxy(Greeter.class, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newProxy(Greeter.class, target, (MethodInterceptor[]) null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newProxy(Greeter.class, target, (MethodInterceptor) null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newProxy(String.class, "not-an-interface"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static <T> T newProxy(
      Class<T> interfaceType, T target, MethodInterceptor... interceptors) {
    return USE_SOLUTION
        ? Kata19_MethodHandleInterceptorSolution.createProxy(interfaceType, target, interceptors)
        : Kata19_MethodHandleInterceptor.createProxy(interfaceType, target, interceptors);
  }
}
