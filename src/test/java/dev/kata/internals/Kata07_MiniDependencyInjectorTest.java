package dev.kata.internals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.internals.Kata07_MiniDependencyInjector.Component;
import dev.kata.internals.Kata07_MiniDependencyInjector.DependencyResolutionException;
import dev.kata.internals.Kata07_MiniDependencyInjector.Inject;
import dev.kata.internals.Kata07_MiniDependencyInjector.Scope;
import dev.kata.solutions.internals.Kata07_MiniDependencyInjectorSolution;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 07.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata07* -DuseSolution=true</pre>
 */
@DisplayName("Kata 07: мини-DI-контейнер")
public class Kata07_MiniDependencyInjectorTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata07_MiniDependencyInjector newContainer(Class<?>... types) {
    return USE_SOLUTION
        ? new Kata07_MiniDependencyInjectorSolution(types)
        : new Kata07_MiniDependencyInjector(types);
  }

  @Test
  @DisplayName("Простой граф: Service получает Repository через конструктор")
  void resolvesSimpleGraph() {
    Kata07_MiniDependencyInjector container = newContainer(Repository.class, Service.class);

    Service service = container.get(Service.class);

    assertThat(service).isNotNull();
    assertThat(service.repository).isNotNull();
  }

  @Test
  @DisplayName("Singleton: один и тот же экземпляр на все запросы")
  void singletonScopeReturnsSameInstance() {
    Kata07_MiniDependencyInjector container = newContainer(Repository.class, Service.class);

    Service first = container.get(Service.class);
    Service second = container.get(Service.class);

    assertThat(first).isSameAs(second);
    assertThat(first.repository).isSameAs(second.repository);
  }

  @Test
  @DisplayName("Prototype: новый экземпляр на каждый запрос")
  void prototypeScopeReturnsNewInstances() {
    Kata07_MiniDependencyInjector container =
        newContainer(Repository.class, PrototypeService.class);

    PrototypeService first = container.get(PrototypeService.class);
    PrototypeService second = container.get(PrototypeService.class);

    assertThat(first).isNotSameAs(second);
    // Зависимость-синглтон при этом общая.
    assertThat(first.repository).isSameAs(second.repository);
  }

  @Test
  @DisplayName("Транзитивный граф: Level1 → Level2 → Level3")
  void resolvesTransitiveGraph() {
    Kata07_MiniDependencyInjector container =
        newContainer(Level1.class, Level2.class, Level3.class);

    Level1 level1 = container.get(Level1.class);

    assertThat(level1.level2.level3).isNotNull();
  }

  @Test
  @DisplayName("Цикл зависимостей: понятное сообщение с полным путём")
  void cyclicDependencyIsReported() {
    Kata07_MiniDependencyInjector container = newContainer(CycleA.class, CycleB.class);

    assertThatThrownBy(() -> container.get(CycleA.class))
        .isInstanceOf(DependencyResolutionException.class)
        .hasMessageContaining("Cyclic dependency")
        .hasMessageContaining(CycleA.class.getName())
        .hasMessageContaining(CycleB.class.getName())
        .hasMessageContaining(" -> ");
  }

  @Test
  @DisplayName("Отсутствующая зависимость: сообщение содержит имя типа")
  void missingDependencyIsReported() {
    Kata07_MiniDependencyInjector container = newContainer(NeedsMissing.class);

    assertThatThrownBy(() -> container.get(NeedsMissing.class))
        .isInstanceOf(DependencyResolutionException.class)
        .hasMessageContaining(MissingDependency.class.getName());
  }

  @Test
  @DisplayName("Незарегистрированный тип нельзя разрешить")
  void unregisteredTypeIsRejected() {
    Kata07_MiniDependencyInjector container = newContainer(Repository.class);

    assertThatThrownBy(() -> container.get(Service.class))
        .isInstanceOf(DependencyResolutionException.class)
        .hasMessageContaining("not registered")
        .hasMessageContaining(Service.class.getName());
  }

  @Test
  @DisplayName("Класс без @Component отвергается")
  void missingComponentAnnotationIsRejected() {
    Kata07_MiniDependencyInjector container = newContainer(Unannotated.class);

    assertThatThrownBy(() -> container.get(Unannotated.class))
        .isInstanceOf(DependencyResolutionException.class)
        .hasMessageContaining("@Component");
  }

  @Test
  @DisplayName("Несколько @Inject-конструкторов — ошибка")
  void multipleInjectConstructorsAreRejected() {
    Kata07_MiniDependencyInjector container =
        newContainer(TwoInjectConstructors.class, Repository.class);

    assertThatThrownBy(() -> container.get(TwoInjectConstructors.class))
        .isInstanceOf(DependencyResolutionException.class)
        .hasMessageContaining("annotated with @Inject");
  }

  @Test
  @DisplayName("Несколько конструкторов без @Inject — ошибка выбора")
  void ambiguousConstructorsAreRejected() {
    Kata07_MiniDependencyInjector container =
        newContainer(AmbiguousConstructors.class, Repository.class);

    assertThatThrownBy(() -> container.get(AmbiguousConstructors.class))
        .isInstanceOf(DependencyResolutionException.class)
        .hasMessageContaining("cannot choose");
  }

  @Test
  @DisplayName("Единственный конструктор без @Inject используется неявно")
  void singleConstructorIsUsedImplicitly() {
    Kata07_MiniDependencyInjector container =
        newContainer(SingleImplicitConstructor.class, Repository.class);

    SingleImplicitConstructor instance = container.get(SingleImplicitConstructor.class);

    assertThat(instance.repository).isNotNull();
  }

  @Test
  @DisplayName("Исключение конструктора сохраняет cause")
  void constructorFailureKeepsCause() {
    Kata07_MiniDependencyInjector container = newContainer(Exploding.class);

    assertThatThrownBy(() -> container.get(Exploding.class))
        .isInstanceOf(DependencyResolutionException.class)
        .hasMessageContaining("boom")
        .hasCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Примитивную зависимость нельзя разрешить — явная ошибка")
  void primitiveDependencyIsRejected() {
    Kata07_MiniDependencyInjector container = newContainer(NeedsPrimitive.class);

    assertThatThrownBy(() -> container.get(NeedsPrimitive.class))
        .isInstanceOf(DependencyResolutionException.class)
        .hasMessageContaining("primitive");
  }

  @Test
  @DisplayName("Потокобезопасность: конкурентные get возвращают один singleton")
  void concurrentSingletonCreation() throws Exception {
    Kata07_MiniDependencyInjector container = newContainer(Repository.class, Service.class);
    int threads = 16;

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Service>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return container.get(Service.class);
                }));
      }
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      Service first = futures.get(0).get(5, TimeUnit.SECONDS);
      for (Future<Service> future : futures) {
        assertThat(future.get(5, TimeUnit.SECONDS)).isSameAs(first);
      }
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("get(null) и дубликаты регистрации")
  void nullTypeAndDuplicateRegistration() {
    Kata07_MiniDependencyInjector container = newContainer(Repository.class, Repository.class);

    assertThat(container.get(Repository.class)).isNotNull();
    assertThatThrownBy(() -> container.get(null)).isInstanceOf(NullPointerException.class);
  }

  // ---------------------------------------------------------------------
  // Тестовые компоненты
  // ---------------------------------------------------------------------

  @Component
  public static class Repository {}

  @Component
  public static class Service {
    final Repository repository;

    @Inject
    public Service(Repository repository) {
      this.repository = repository;
    }
  }

  @Component(scope = Scope.PROTOTYPE)
  public static class PrototypeService {
    final Repository repository;

    @Inject
    public PrototypeService(Repository repository) {
      this.repository = repository;
    }
  }

  @Component
  public static class Level3 {}

  @Component
  public static class Level2 {
    final Level3 level3;

    @Inject
    public Level2(Level3 level3) {
      this.level3 = level3;
    }
  }

  @Component
  public static class Level1 {
    final Level2 level2;

    @Inject
    public Level1(Level2 level2) {
      this.level2 = level2;
    }
  }

  @Component
  public static class CycleA {
    @Inject
    public CycleA(CycleB cycleB) {}
  }

  @Component
  public static class CycleB {
    @Inject
    public CycleB(CycleA cycleA) {}
  }

  public interface MissingDependency {}

  @Component
  public static class NeedsMissing {
    @Inject
    public NeedsMissing(MissingDependency missing) {}
  }

  public static class Unannotated {}

  @Component
  public static class TwoInjectConstructors {
    @Inject
    public TwoInjectConstructors() {}

    @Inject
    public TwoInjectConstructors(Repository repository) {}
  }

  @Component
  public static class AmbiguousConstructors {
    public AmbiguousConstructors() {}

    public AmbiguousConstructors(Repository repository) {}
  }

  @Component
  public static class SingleImplicitConstructor {
    final Repository repository;

    public SingleImplicitConstructor(Repository repository) {
      this.repository = repository;
    }
  }

  @Component
  public static class Exploding {
    @Inject
    public Exploding() {
      throw new IllegalStateException("boom");
    }
  }

  @Component
  public static class NeedsPrimitive {
    @Inject
    public NeedsPrimitive(int value) {}
  }
}
