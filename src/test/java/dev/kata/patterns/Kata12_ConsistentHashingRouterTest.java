package dev.kata.patterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.patterns.Kata12_ConsistentHashingRouterSolution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
 * Тесты Kata 12.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata12* -DuseSolution=true</pre>
 */
@DisplayName("Kata 12: консистентное хеширование с виртуальными нодами")
class Kata12_ConsistentHashingRouterTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final int KEY_COUNT = 100_000;

  private Kata12_ConsistentHashingRouter newRouter(int virtualNodesCount) {
    return USE_SOLUTION
        ? new Kata12_ConsistentHashingRouterSolution(virtualNodesCount)
        : new Kata12_ConsistentHashingRouter(virtualNodesCount);
  }

  @Test
  @DisplayName("Пустой кластер: route возвращает empty")
  void emptyCluster() {
    Kata12_ConsistentHashingRouter router = newRouter(100);

    assertThat(router.route("any-key")).isEmpty();
    assertThat(router.nodes()).isEmpty();
  }

  @Test
  @DisplayName("Один узел: все ключи маршрутизируются в него")
  void singleNodeOwnsEveryKey() {
    Kata12_ConsistentHashingRouter router = newRouter(100);
    router.addNode("node-0");

    for (int i = 0; i < 10_000; i++) {
      assertThat(router.route("key-" + i)).contains("node-0");
    }
    assertThat(router.nodes()).containsExactly("node-0");
  }

  @Test
  @DisplayName("Равномерность: 100 000 ключей на 5 нод отклоняются от 20% не более чем на 4%")
  void distributionIsUniformAcrossFiveNodes() {
    Kata12_ConsistentHashingRouter router = newRouter(200);
    addNodes(router, 5);

    Map<String, Integer> counts = new HashMap<>();
    for (int i = 0; i < KEY_COUNT; i++) {
      String node = route(router, "key-" + i);
      counts.merge(node, 1, Integer::sum);
    }

    assertThat(counts).hasSize(5);
    for (String node : router.nodes()) {
      double share = counts.get(node) / (double) KEY_COUNT;
      assertThat(share).as("доля узла %s", node).isBetween(0.16, 0.24);
    }
  }

  @Test
  @DisplayName("Добавление шестой ноды перевозит только ~1/6 ключей и только на новую ноду")
  void addingNodeMovesOnlyItsShare() {
    Kata12_ConsistentHashingRouter router = newRouter(200);
    addNodes(router, 5);

    String[] before = new String[KEY_COUNT];
    for (int i = 0; i < KEY_COUNT; i++) {
      before[i] = route(router, "key-" + i);
    }

    router.addNode("node-5");
    assertThat(router.nodes()).hasSize(6);

    int moved = 0;
    for (int i = 0; i < KEY_COUNT; i++) {
      String after = route(router, "key-" + i);
      if (!after.equals(before[i])) {
        moved++;
        assertThat(after).as("переехавший ключ key-%d", i).isEqualTo("node-5");
      }
    }

    double movedShare = moved / (double) KEY_COUNT;
    assertThat(movedShare).as("доля переехавших ключей").isBetween(0.12, 0.22);
  }

  @Test
  @DisplayName("Удаление ноды перевозит только её ключи, остальные не меняются")
  void removingNodeMovesOnlyItsKeys() {
    Kata12_ConsistentHashingRouter router = newRouter(200);
    addNodes(router, 6);

    String[] before = new String[KEY_COUNT];
    for (int i = 0; i < KEY_COUNT; i++) {
      before[i] = route(router, "key-" + i);
    }

    router.removeNode("node-2");
    assertThat(router.nodes()).hasSize(5).doesNotContain("node-2");

    int moved = 0;
    for (int i = 0; i < KEY_COUNT; i++) {
      String after = route(router, "key-" + i);
      assertThat(after).isNotEqualTo("node-2");
      if (!after.equals(before[i])) {
        moved++;
        assertThat(before[i]).as("переехавший ключ key-%d", i).isEqualTo("node-2");
      }
    }

    double movedShare = moved / (double) KEY_COUNT;
    assertThat(movedShare).as("доля переехавших ключей").isBetween(0.12, 0.22);
  }

  @Test
  @DisplayName("Повторное добавление узла и удаление отсутствующего — no-op")
  void duplicateAddAndMissingRemoveAreIdempotent() {
    Kata12_ConsistentHashingRouter router = newRouter(100);
    addNodes(router, 3);

    String[] before = new String[10_000];
    for (int i = 0; i < before.length; i++) {
      before[i] = route(router, "key-" + i);
    }

    router.addNode("node-1"); // уже есть
    router.removeNode("does-not-exist");

    assertThat(router.nodes()).containsExactlyInAnyOrder("node-0", "node-1", "node-2");
    for (int i = 0; i < before.length; i++) {
      assertThat(route(router, "key-" + i)).isEqualTo(before[i]);
    }
  }

  @Test
  @DisplayName("Маршрутизация детерминирована при неизменной топологии")
  void routingIsDeterministic() {
    Kata12_ConsistentHashingRouter router = newRouter(64);
    addNodes(router, 4);

    for (int i = 0; i < 1_000; i++) {
      String key = "stable-key-" + i;
      String first = route(router, key);
      for (int attempt = 0; attempt < 5; attempt++) {
        assertThat(route(router, key)).isEqualTo(first);
      }
    }
  }

  @Test
  @DisplayName("Чтения во время обновлений топологии не падают и видят целостный снимок")
  void concurrentRoutingAndTopologyUpdates() throws Exception {
    Kata12_ConsistentHashingRouter router = newRouter(100);
    router.addNode("stable");
    Set<String> knownNodes = ConcurrentHashMap.newKeySet();
    knownNodes.add("stable");

    int readers = 4;
    int routesPerReader = 50_000;
    AtomicReference<Throwable> failure = new AtomicReference<>();
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(readers + 1);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int r = 0; r < readers; r++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  for (int i = 0; i < routesPerReader; i++) {
                    try {
                      String node = route(router, "key-" + i);
                      if (!knownNodes.contains(node)) {
                        failure.compareAndSet(
                            null, new AssertionError("unknown node returned: " + node));
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
                for (int i = 0; i < 50; i++) {
                  String nodeId = "temp-" + i;
                  knownNodes.add(nodeId);
                  router.addNode(nodeId);
                  router.removeNode(nodeId);
                }
                return null;
              });
      futures.add(writer);

      start.countDown();
      for (Future<?> future : futures) {
        future.get(60, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(failure.get()).isNull();
    assertThat(router.nodes()).containsExactly("stable");
    for (int i = 0; i < 1_000; i++) {
      assertThat(route(router, "key-" + i)).isEqualTo("stable");
    }
  }

  @Test
  @DisplayName("nodes() — неизменяемый снимок")
  void nodesSnapshotIsUnmodifiable() {
    Kata12_ConsistentHashingRouter router = newRouter(10);
    router.addNode("node-0");

    Set<String> snapshot = router.nodes();
    assertThat(snapshot).containsExactly("node-0");
    assertThatThrownBy(() -> snapshot.add("hacked"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("null-аргументы запрещены")
  void nullsRejected() {
    Kata12_ConsistentHashingRouter router = newRouter(10);

    assertThatThrownBy(() -> router.addNode(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> router.removeNode(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> router.route(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("virtualNodesCount должен быть больше нуля")
  void invalidVirtualNodesCount() {
    assertThatThrownBy(() -> newRouter(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newRouter(-1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Каждый ключ всегда обслуживается одним из добавленных узлов")
  void everyKeyIsServedByAnExistingNode() {
    Kata12_ConsistentHashingRouter router = newRouter(50);
    addNodes(router, 7);

    Set<String> nodes = new HashSet<>(router.nodes());
    for (int i = 0; i < 20_000; i++) {
      assertThat(nodes).contains(route(router, "key-" + i));
    }
  }

  private static void addNodes(Kata12_ConsistentHashingRouter router, int count) {
    for (int i = 0; i < count; i++) {
      router.addNode("node-" + i);
    }
  }

  private static String route(Kata12_ConsistentHashingRouter router, String key) {
    return router.route(key).orElseThrow(() -> new AssertionError("no node for " + key));
  }
}
