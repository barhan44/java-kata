package dev.kata.benchmarks;

import dev.kata.concurrency.Kata01_StripedLockCache;
import dev.kata.solutions.concurrency.Kata01_StripedLockCacheSolution;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
 * Бенчмарк Kata 01: lock-striped кэш против {@code Collections.synchronizedMap}.
 *
 * <h2>Нагрузка</h2>
 *
 * <p>8 потоков, 1024 ключа, 90% чтений и 10% записей — типичный профиль кэша справочных данных. Обе
 * реализации предварительно заполнены, поэтому чтения попадают в кэш и не вызывают загрузчик.
 * Сравниваем:
 *
 * <ul>
 *   <li>{@link Kata01_StripedLockCache} — 64 полосы, чтение под {@code ReadWriteLock}, {@code
 *       LongAdder} для статистики;
 *   <li>{@link Collections#synchronizedMap(Map)} — один монитор на весь {@code HashMap}, поэтому
 *       все 8 потоков сериализуются на каждой операции.
 * </ul>
 *
 * <h2>Почему запись — это {@code put}, а чтение — {@code get(key, loader)}</h2>
 *
 * <p>Для кэша используется штатный путь чтения с загрузчиком (он не вызывается, потому что ключ уже
 * есть). {@code getIfPresent} не подходит: он возвращает {@code Optional} и добавляет аллокацию,
 * которая исказила бы сравнение. {@code synchronizedMap.get} — прямой аналог чтения без загрузчика.
 *
 * <h2>Разбор аннотаций JMH</h2>
 *
 * <ul>
 *   <li>{@link Threads}{@code (8)} на классе — все бенчмарки запускаются в 8 потоках, разделяющих
 *       один {@link State}{@code (Scope.Benchmark)}. Это и есть точка конкуренции: общий кэш на
 *       всех.
 *   <li>{@link BenchmarkMode} — одновременно throughput (ops/µs) и average time (µs/op): первое
 *       показывает масштабирование, второе — задержку.
 *   <li>{@link OutputTimeUnit}{@code (TimeUnit.MICROSECONDS)} — удобно для операций длительностью
 *       десятки-сотни наносекунд.
 *   <li>{@link Warmup} / {@link Measurement} / {@link Fork} — быстрый учебный профиль; для
 *       серьёзных выводов увеличьте число итераций и форков.
 *   <li>{@link Setup}{@code (Level.Trial)} — заполнение выполняется один раз на форк, а не на
 *       итерацию: иначе {@code put} 1024 ключей попал бы в измерения.
 * </ul>
 *
 * <h2>Подводные камни интерпретации</h2>
 *
 * <ul>
 *   <li>{@link ThreadLocalRandom} в каждом вызове добавляет ~5–10 нс к обеим реализациям; это общий
 *       для них шум, но он «съедает» часть разницы. Для точного сравнения можно вынести генератор в
 *       {@code Scope.Thread} state.
 *   <li>Синтетический {@code HashMap} под монитором на 8 потоках — худший случай для {@code
 *       synchronizedMap}. Реальный {@code ConcurrentHashMap} был бы заметно лучше; здесь он не
 *       используется, потому что задача — показать эффект striping.
 *   <li>Все ключи помещаются в 64 полосы, то есть в среднем 16 ключей на полосу. При меньшем числе
 *       полос contention растёт, при большем — падает.
 *   <li>GC-профилировщик покажет аллокации: у {@code synchronizedMap.get} их нет, у кэша на промахе
 *       создаётся {@code Optional}/запись — но в этой нагрузке промахов нет.
 * </ul>
 *
 * <h2>Запуск</h2>
 *
 * <pre>
 * mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
 *     -Dexec.args="StripedCache" -DuseSolution=true
 * </pre>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@Threads(8)
@State(Scope.Benchmark)
public class StripedCacheBenchmark {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final int KEY_COUNT = 1024;
  private static final int STRIPES = 64;
  private static final long TTL_MILLIS = 60_000;

  private Kata01_StripedLockCache<Integer, Integer> cache;
  private Map<Integer, Integer> synchronizedMap;

  @Setup(Level.Trial)
  public void setUp() {
    cache =
        USE_SOLUTION
            ? new Kata01_StripedLockCacheSolution<>(STRIPES, TTL_MILLIS)
            : new Kata01_StripedLockCache<>(STRIPES, TTL_MILLIS);
    synchronizedMap = Collections.synchronizedMap(new HashMap<>());

    for (int key = 0; key < KEY_COUNT; key++) {
      cache.put(key, key);
      synchronizedMap.put(key, key);
    }
  }

  /** 90% чтений / 10% записей через striped-кэш. */
  @Benchmark
  public int stripedCache() {
    int key = ThreadLocalRandom.current().nextInt(KEY_COUNT);
    if (ThreadLocalRandom.current().nextInt(10) == 0) {
      cache.put(key, key);
      return key;
    }
    return cache.get(key, k -> k);
  }

  /** Та же нагрузка через один глобальный монитор. */
  @Benchmark
  public int synchronizedMap() {
    int key = ThreadLocalRandom.current().nextInt(KEY_COUNT);
    if (ThreadLocalRandom.current().nextInt(10) == 0) {
      synchronizedMap.put(key, key);
      return key;
    }
    Integer value = synchronizedMap.get(key);
    return value == null ? -1 : value;
  }
}
