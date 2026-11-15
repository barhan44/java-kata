package dev.kata.algorithms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.algorithms.Kata17_RadixUrlRouter.RouteMatch;
import dev.kata.solutions.algorithms.Kata17_RadixUrlRouterSolution;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 17.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata17* -DuseSolution=true</pre>
 */
@DisplayName("Kata 17: radix tree роутер URL")
class Kata17_RadixUrlRouterTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata17_RadixUrlRouter<String> newRouter() {
    return USE_SOLUTION ? new Kata17_RadixUrlRouterSolution<>() : new Kata17_RadixUrlRouter<>();
  }

  @Test
  @DisplayName("Статические маршруты и расщепление общих префиксов")
  void staticRoutesAndSplitting() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/api/users", "users");
    router.addRoute("GET", "/api/orders", "orders");
    router.addRoute("GET", "/api/user", "user");
    router.addRoute("GET", "/api/user/profile", "profile");

    assertThat(handler(router, "GET", "/api/users")).isEqualTo("users");
    assertThat(handler(router, "GET", "/api/orders")).isEqualTo("orders");
    assertThat(handler(router, "GET", "/api/user")).isEqualTo("user");
    assertThat(handler(router, "GET", "/api/user/profile")).isEqualTo("profile");

    assertThat(router.match("GET", "/api/us")).isEmpty();
    assertThat(router.match("GET", "/api/usersX")).isEmpty();
    assertThat(router.match("GET", "/api/user/")).isEmpty();
    assertThat(router.match("GET", "/api")).isEmpty();
  }

  @Test
  @DisplayName("Корневой маршрут и пустой роутер")
  void rootAndEmptyRouter() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    assertThat(router.match("GET", "/")).isEmpty();

    router.addRoute("GET", "/", "root");
    assertThat(handler(router, "GET", "/")).isEqualTo("root");
    assertThat(router.match("GET", "/x")).isEmpty();
  }

  @Test
  @DisplayName("Параметры пути извлекаются по именам в правильном порядке")
  void pathParameters() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/users/{id}", "user");
    router.addRoute("GET", "/users/{id}/orders/{orderId}", "order");

    RouteMatch<String> user = match(router, "GET", "/users/42");
    assertThat(user.handler()).isEqualTo("user");
    assertThat(user.pathParams()).containsExactlyInAnyOrderEntriesOf(Map.of("id", "42"));

    RouteMatch<String> order = match(router, "GET", "/users/42/orders/99");
    assertThat(order.handler()).isEqualTo("order");
    assertThat(order.pathParams())
        .containsExactlyInAnyOrderEntriesOf(Map.of("id", "42", "orderId", "99"));
    assertThat(order.pathParams().keySet()).containsExactly("id", "orderId");

    assertThat(router.match("GET", "/users/42/orders")).isEmpty();
    assertThat(router.match("GET", "/users//orders/99")).isEmpty();
    assertThat(router.match("GET", "/users/42/orders/99/x")).isEmpty();
  }

  @Test
  @DisplayName("Wildcard захватывает остаток пути, включая слэши")
  void wildcardTail() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/files/*filepath", "files");
    router.addRoute("GET", "/files", "root-files");

    RouteMatch<String> nested = match(router, "GET", "/files/a/b.txt");
    assertThat(nested.handler()).isEqualTo("files");
    assertThat(nested.pathParams()).containsEntry("filepath", "a/b.txt");

    RouteMatch<String> empty = match(router, "GET", "/files/");
    assertThat(empty.handler()).isEqualTo("files");
    assertThat(empty.pathParams()).containsEntry("filepath", "");

    assertThat(handler(router, "GET", "/files")).isEqualTo("root-files");
    assertThat(router.match("GET", "/filesX")).isEmpty();
  }

  @Test
  @DisplayName("Приоритет: exact > parameter > wildcard")
  void matchPriority() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/users/me", "exact");
    router.addRoute("GET", "/users/{id}", "param");
    router.addRoute("GET", "/users/*rest", "wildcard");

    RouteMatch<String> exact = match(router, "GET", "/users/me");
    assertThat(exact.handler()).isEqualTo("exact");
    assertThat(exact.pathParams()).isEmpty();

    RouteMatch<String> param = match(router, "GET", "/users/42");
    assertThat(param.handler()).isEqualTo("param");
    assertThat(param.pathParams()).containsEntry("id", "42");

    RouteMatch<String> wildcard = match(router, "GET", "/users/42/extra");
    assertThat(wildcard.handler()).isEqualTo("wildcard");
    assertThat(wildcard.pathParams()).containsEntry("rest", "42/extra");
  }

  @Test
  @DisplayName("Backtracking: статическая ветка без handler'а уступает параметру")
  void backtracksFromStaticToParam() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/users/me/profile", "profile");
    router.addRoute("GET", "/users/{id}", "user");

    assertThat(handler(router, "GET", "/users/me/profile")).isEqualTo("profile");
    RouteMatch<String> me = match(router, "GET", "/users/me");
    assertThat(me.handler()).isEqualTo("user");
    assertThat(me.pathParams()).containsEntry("id", "me");
  }

  @Test
  @DisplayName("Backtracking при несовпадении общего префикса")
  void backtracksAfterPrefixMismatch() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/api/users", "users");
    router.addRoute("GET", "/{scope}/orders", "scoped-orders");

    RouteMatch<String> match = match(router, "GET", "/api/orders");
    assertThat(match.handler()).isEqualTo("scoped-orders");
    assertThat(match.pathParams()).containsEntry("scope", "api");
  }

  @Test
  @DisplayName("HTTP-методы независимы; отсутствие метода не блокирует fallback")
  void methodSpecificRoutes() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/users/{id}", "get-user");
    router.addRoute("POST", "/users/{id}", "post-user");
    router.addRoute("GET", "/users/me", "get-me");

    assertThat(handler(router, "GET", "/users/42")).isEqualTo("get-user");
    assertThat(handler(router, "POST", "/users/42")).isEqualTo("post-user");
    assertThat(router.match("DELETE", "/users/42")).isEmpty();

    RouteMatch<String> postMe = match(router, "POST", "/users/me");
    assertThat(postMe.handler()).isEqualTo("post-user");
    assertThat(postMe.pathParams()).containsEntry("id", "me");
  }

  @Test
  @DisplayName("Query string и fragment игнорируются")
  void queryAndFragmentAreIgnored() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/users/{id}", "user");

    assertThat(match(router, "GET", "/users/42?fields=name").pathParams())
        .containsEntry("id", "42");
    assertThat(match(router, "GET", "/users/42#top").pathParams()).containsEntry("id", "42");
    assertThat(match(router, "GET", "/users/42?a=1#top").pathParams()).containsEntry("id", "42");
  }

  @Test
  @DisplayName("Корневой wildcard и приоритет точного корня")
  void rootWildcard() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/*all", "all");

    assertThat(match(router, "GET", "/").pathParams()).containsEntry("all", "");
    assertThat(match(router, "GET", "/foo").pathParams()).containsEntry("all", "foo");
    assertThat(match(router, "GET", "/foo/bar").pathParams()).containsEntry("all", "foo/bar");

    router.addRoute("GET", "/", "root");
    assertThat(handler(router, "GET", "/")).isEqualTo("root");
    assertThat(handler(router, "GET", "/foo")).isEqualTo("all");
  }

  @Test
  @DisplayName("Конфликт имён параметров не меняет дерево")
  void parameterNameConflictIsAtomic() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/a/{id}", "original");

    assertThatThrownBy(() -> router.addRoute("GET", "/a/{name}", "conflict"))
        .isInstanceOf(IllegalArgumentException.class);

    RouteMatch<String> match = match(router, "GET", "/a/42");
    assertThat(match.handler()).isEqualTo("original");
    assertThat(match.pathParams()).containsEntry("id", "42");
  }

  @Test
  @DisplayName("Конфликт wildcard-имён отвергается")
  void wildcardNameConflict() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/f/*path", "files");

    assertThatThrownBy(() -> router.addRoute("GET", "/f/*other", "other"))
        .isInstanceOf(IllegalArgumentException.class);

    router.addRoute("GET", "/f/", "slash");
    assertThat(handler(router, "GET", "/f/")).isEqualTo("slash");
  }

  @Test
  @DisplayName("Некорректные pattern'ы отвергаются")
  void invalidPatterns() {
    Kata17_RadixUrlRouter<String> router = newRouter();

    for (String invalid :
        List.of(
            "",
            "users",
            "/a/{id",
            "/a/{}",
            "/a/{id}x",
            "/a/x{id}",
            "/a/{id}/{id}",
            "/a/*x/y",
            "/a/*",
            "/a/**",
            "/a/{id}?x",
            "/a?x",
            "/a#x")) {
      assertThatThrownBy(() -> router.addRoute("GET", invalid, "h"))
          .as("pattern '%s'", invalid)
          .isInstanceOf(IllegalArgumentException.class);
    }

    assertThatThrownBy(() -> router.addRoute("", "/a", "h"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> router.addRoute(null, "/a", "h"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> router.addRoute("GET", null, "h"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> router.addRoute("GET", "/a", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> router.match(null, "/a")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> router.match("GET", null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Похожие префиксы не дают ложных срабатываний")
  void similarPrefixes() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/api/users", "users");
    router.addRoute("GET", "/api/user", "user");
    router.addRoute("GET", "/api/user/profile", "profile");
    router.addRoute("GET", "/api/user/{id}", "user-by-id");

    assertThat(handler(router, "GET", "/api/users")).isEqualTo("users");
    assertThat(handler(router, "GET", "/api/user")).isEqualTo("user");
    assertThat(handler(router, "GET", "/api/user/profile")).isEqualTo("profile");
    assertThat(handler(router, "GET", "/api/user/42")).isEqualTo("user-by-id");

    assertThat(router.match("GET", "/api/userx")).isEmpty();
    assertThat(router.match("GET", "/api/users/42")).isEmpty();
  }

  @Test
  @DisplayName("Трейлинг-слэш значим")
  void trailingSlashIsSignificant() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/a", "plain");

    assertThat(handler(router, "GET", "/a")).isEqualTo("plain");
    assertThat(router.match("GET", "/a/")).isEmpty();

    router.addRoute("GET", "/a/", "slash");
    assertThat(handler(router, "GET", "/a/")).isEqualTo("slash");
  }

  @Test
  @DisplayName("Параметры в RouteMatch неизменяемы")
  void matchParametersAreImmutable() {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/users/{id}", "user");

    Map<String, String> params = match(router, "GET", "/users/42").pathParams();
    assertThatThrownBy(() -> params.put("x", "y"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Чтения во время добавления маршрутов потокобезопасны")
  void concurrentReadsAndWrites() throws Exception {
    Kata17_RadixUrlRouter<String> router = newRouter();
    router.addRoute("GET", "/static/{id}", "static");

    int readers = 4;
    int matchesPerReader = 20_000;
    AtomicReference<Throwable> failure = new AtomicReference<>();
    CountDownLatch start = new CountDownLatch(1);
    Set<String> knownHandlers = ConcurrentHashMap.newKeySet();
    knownHandlers.add("static");

    ExecutorService pool = Executors.newFixedThreadPool(readers + 1);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int r = 0; r < readers; r++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  for (int i = 0; i < matchesPerReader; i++) {
                    try {
                      RouteMatch<String> match = router.match("GET", "/static/" + i).orElseThrow();
                      if (!knownHandlers.contains(match.handler())) {
                        failure.compareAndSet(
                            null, new AssertionError("unknown handler: " + match.handler()));
                      }
                    } catch (Throwable t) {
                      failure.compareAndSet(null, t);
                      return null;
                    }
                  }
                  return null;
                }));
      }

      Future<?> writer =
          pool.submit(
              () -> {
                start.await();
                for (int i = 0; i < 200; i++) {
                  String handler = "dynamic-" + i;
                  knownHandlers.add(handler);
                  router.addRoute("GET", "/dynamic/" + i, handler);
                }
                return null;
              });
      futures.add(writer);

      start.countDown();
      for (Future<?> future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(failure.get()).isNull();
    assertThat(handler(router, "GET", "/dynamic/199")).isEqualTo("dynamic-199");
  }

  private static String handler(Kata17_RadixUrlRouter<String> router, String method, String path) {
    return match(router, method, path).handler();
  }

  private static RouteMatch<String> match(
      Kata17_RadixUrlRouter<String> router, String method, String path) {
    return router
        .match(method, path)
        .orElseThrow(() -> new AssertionError("no route for " + method + " " + path));
  }
}
