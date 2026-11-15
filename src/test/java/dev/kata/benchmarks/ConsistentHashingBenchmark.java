package dev.kata.benchmarks;

import dev.kata.patterns.Kata12_ConsistentHashingRouter;
import dev.kata.solutions.patterns.Kata12_ConsistentHashingRouterSolution;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Бенчмарк Kata 12: маршрутизация через кольцо консистентного хеширования.
 *
 * <h2>Сценарий</h2>
 *
 * <p>5 узлов × 200 виртуальных нод, 4096 ключей, 8 потоков. Сравниваются:
 *
 * <ul>
 *   <li>{@code consistentHashing} — {@link Kata12_ConsistentHashingRouter#route(String)}: хеш по
 *       строке + бинарный поиск по {@code long[]};
 *   <li>{@code moduloBaseline} — {@code nodes[floorMod(key.hashCode(), 5)]}: нижняя граница
 *       стоимости, но с полной перебалансировкой при изменении числа узлов.
 * </ul>
 *
 * <p>Цель — показать цену стабильности: O(log V) вместо O(1), но без миграции 5/6 ключей при
 * добавлении узла.
 *
 * <h2>Аллокации</h2>
 *
 * <p>Обе реализации на горячем пути не должны аллоцировать. У {@code route} {@link Optional}
 * кэшируется на узел при обновлении топологии, поэтому {@code gc.alloc.rate.norm} должен быть около
 * нуля. Если в результатах появились байты на операцию — скорее всего, реализация создаёт {@code
 * Optional.of} или боксит ключ для {@code TreeMap}.
 *
 * <h2>Аннотации</h2>
 *
 * <ul>
 *   <li>{@link State}{@code (Scope.Benchmark)} — общее кольцо и массив ключей на все потоки: это
 *       честная нагрузка «один роутер на сервис».
 *   <li>{@link State}{@code (Scope.Thread)} — свой индекс обхода ключей у каждого потока, чтобы
 *       избежать ложного sharing-а на счётчике.
 *   <li>{@link Threads}{@code (8)} — конкуренция за чтение immutable-снимка.
 *   <li>{@link Warmup}/{@link Measurement}/{@link Fork} — учебный профиль, ~10–20 секунд.
 * </ul>
 *
 * <h2>Запуск</h2>
 *
 * <pre>
 * mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
 *     -Dexec.args="ConsistentHashing" -DuseSolution=true
 * </pre>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@Threads(8)
public class ConsistentHashingBenchmark {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final int NODE_COUNT = 5;
  private static final int VIRTUAL_NODES = 200;
  private static final int KEY_COUNT = 4096; // степень двойки: дешёвая маска индекса

  /** Общее состояние: кольцо, ключи и узлы для baseline. */
  @State(Scope.Benchmark)
  public static class Shared {
    Kata12_ConsistentHashingRouter router;
    String[] keys;
    String[] nodes;

    @Setup(Level.Trial)
    public void setUp() {
      router =
          USE_SOLUTION
              ? new Kata12_ConsistentHashingRouterSolution(VIRTUAL_NODES)
              : new Kata12_ConsistentHashingRouter(VIRTUAL_NODES);

      nodes = new String[NODE_COUNT];
      for (int i = 0; i < NODE_COUNT; i++) {
        nodes[i] = "node-" + i;
        router.addNode(nodes[i]);
      }

      keys = new String[KEY_COUNT];
      for (int i = 0; i < KEY_COUNT; i++) {
        keys[i] = "key-" + i;
      }
    }
  }

  /** Свой индекс у каждого потока: нет contention на общем счётчике. */
  @State(Scope.Thread)
  public static class Local {
    int index;
  }

  /** Маршрутизация через кольцо консистентного хеширования. */
  @Benchmark
  public Optional<String> consistentHashing(Shared shared, Local local) {
    return shared.router.route(shared.keys[local.index++ & (KEY_COUNT - 1)]);
  }

  /** Нижняя граница: modulo по hashCode, без стабильности при изменении топологии. */
  @Benchmark
  public String moduloBaseline(Shared shared, Local local) {
    String key = shared.keys[local.index++ & (KEY_COUNT - 1)];
    return shared.nodes[Math.floorMod(key.hashCode(), NODE_COUNT)];
  }
}
