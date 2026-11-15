package dev.kata.benchmarks;

import dev.kata.patterns.Kata03_TokenBucketRateLimiter;
import dev.kata.solutions.patterns.Kata03_TokenBucketRateLimiterSolution;
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
 * Бенчмарк Kata 03: Token Bucket rate limiter.
 *
 * <h2>Что и зачем измеряем</h2>
 *
 * <p>Сравниваем пропускную способность ({@link Mode#Throughput}) и среднюю задержку ({@link
 * Mode#AverageTime}) одного и того же limiter'а в 1 и 8 потоках. В одно потоке видна «чистая»
 * стоимость CAS-цикла; в восьми — цена конкуренции за одну атомарную ссылку. Именно на 8 потоках
 * проявляется разница между lock-free реализацией и, например, вариантом на {@code synchronized}.
 *
 * <h2>Разбор аннотаций JMH</h2>
 *
 * <ul>
 *   <li>{@link BenchmarkMode} — какие метрики собирать. {@code Throughput} — операций в единицу
 *       времени (больше лучше), {@code AverageTime} — среднее время одной операции (меньше лучше).
 *       Обе метрики на одном прогоне полезны: throughput показывает масштабирование, average time —
 *       задержку отдельного вызова.
 *   <li>{@link OutputTimeUnit} — единица вывода. Микросекунды удобны для наносекундных операций:
 *       результат читается как «0.02 µs/op» вместо «20 ns/op».
 *   <li>{@link Warmup} — 2 итерации по 1 секунде. JIT компилирует метод только после нескольких
 *       тысяч вызовов; без прогрева первые измерения отражали бы интерпретируемый режим.
 *   <li>{@link Measurement} — 3 итерации по 1 секунде. JMH покажет разброс между итерациями; трёх
 *       достаточно для учебного профиля, для продакшн-выводов нужно 5–10.
 *   <li>{@link Fork} — 1 отдельный процесс JVM. Форк изолирует бенчмарк от состояния Maven/IDE и от
 *       профилировщика; больше форков уменьшают случайный шум, но умножают время прогона.
 *   <li>{@link State} — объект, который JMH создаёт и передаёт в бенчмарк-методы. {@code
 *       Scope.Benchmark} означает один общий экземпляр на весь бенчмарк (все потоки видят один
 *       limiter). Для сравнения «на одном состоянии» это то, что нужно.
 *   <li>{@link Threads} на методе — число потоков именно для этого бенчмарка. Так в одном классе
 *       можно честно сравнить 1 и 8 потоков, не дублируя код.
 *   <li>{@link Setup} с {@code Level.Iteration} — новый limiter перед каждой итерацией, чтобы ведро
 *       начиналось полным и измерения не зависели от предыдущей итерации.
 * </ul>
 *
 * <h2>Параметры</h2>
 *
 * <p>Ёмкость и скорость пополнения заведомо выше пропускной способности одного потока, поэтому
 * почти все вызовы успешны и мы измеряем «горячий» путь с обновлением состояния. Чтобы измерить
 * путь отказа, уменьшите {@code REFILL_PER_SECOND} до, например, 1000.
 *
 * <h2>Запуск</h2>
 *
 * <pre>
 * mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
 *     -Dexec.args="RateLimiter" -DuseSolution=true
 * </pre>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class RateLimiterBenchmark {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final long CAPACITY = 1_000_000_000L;
  private static final double REFILL_PER_SECOND = 1_000_000_000.0;

  private Kata03_TokenBucketRateLimiter limiter;

  @Setup(Level.Iteration)
  public void setUp() {
    limiter =
        USE_SOLUTION
            ? new Kata03_TokenBucketRateLimiterSolution(CAPACITY, REFILL_PER_SECOND)
            : new Kata03_TokenBucketRateLimiter(CAPACITY, REFILL_PER_SECOND);
  }

  /** Один поток: стоимость CAS-цикла без конкуренции. */
  @Benchmark
  @Threads(1)
  public boolean tryAcquireSingleThread() {
    return limiter.tryAcquire();
  }

  /** Восемь потоков: конкуренция за одну и ту же атомарную ссылку. */
  @Benchmark
  @Threads(8)
  public boolean tryAcquireEightThreads() {
    return limiter.tryAcquire();
  }
}
