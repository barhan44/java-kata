package dev.kata.benchmarks;

import dev.kata.distributed.Kata23_HybridLogicalClock;
import dev.kata.distributed.Kata23_HybridLogicalClock.HlcTimestamp;
import dev.kata.solutions.distributed.Kata23_HybridLogicalClockSolution;
import dev.kata.solutions.storage.Kata20_WriteAheadLogSolution;
import dev.kata.storage.Kata20_WriteAheadLog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Бенчмарк Kata 23 (HLC) и Kata 20 (WAL).
 *
 * <h2>HLC: упаковка без аллокаций</h2>
 *
 * <p>{@code hlcNowPacked} и {@code hlcUpdatePacked} работают только с {@code long}/{@code int} и не
 * создают объектов. {@code GCProfiler} должен показать {@code gc.alloc.rate.norm ≈ 0 B/op}. Если
 * появились байты на операцию — значит, реализация создаёт {@link HlcTimestamp} там, где обещан
 * примитивный путь. Часы у каждого потока свои ({@code Scope.Thread}), поэтому synchronized-методы
 * не конкурируют.
 *
 * <h2>WAL: последовательная запись</h2>
 *
 * <p>{@code walAppend} пишет 64-байтный payload и делает {@code force(false)} на каждый вызов. Это
 * измеряет реальную стоимость durability: fsync вносит вклад в десятки микросекунд и зависит от
 * файловой системы. Каждый поток использует свой файл, чтобы избежать общей блокировки WAL. {@code
 * gc.alloc.rate.norm} здесь не ноль: на кадр создаются metadata/frame буферы; это ожидаемо.
 *
 * <h2>Запуск</h2>
 *
 * <pre>
 * mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
 *     -Dexec.args="WalAndHlc" -DuseSolution=true
 * </pre>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@Threads(4)
public class WalAndHlcBenchmark {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  /** Свои часы на каждый поток: нет конкуренции за synchronized-монитор. */
  @State(Scope.Thread)
  public static class HlcState {
    Kata23_HybridLogicalClock clock;
    long remotePacked;

    @Setup(Level.Trial)
    public void setUp() {
      AtomicLong physical = new AtomicLong(1_700_000_000_000L);
      clock =
          USE_SOLUTION
              ? new Kata23_HybridLogicalClockSolution(physical::get)
              : new Kata23_HybridLogicalClock(physical::get);
      remotePacked = pack(new HlcTimestamp(1_700_000_000_001L, 7));
    }
  }

  /** Свой WAL-файл на каждый поток: измеряем запись, а не lock. */
  @State(Scope.Thread)
  public static class WalState {
    Kata20_WriteAheadLog wal;
    Path file;
    byte[] payload = new byte[64];

    @Setup(Level.Trial)
    public void setUp() throws IOException {
      file = Files.createTempFile("kata20-wal-", ".log");
      wal = USE_SOLUTION ? new Kata20_WriteAheadLogSolution(file) : new Kata20_WriteAheadLog(file);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
      wal.close();
      Files.deleteIfExists(file);
    }
  }

  @Benchmark
  public long hlcNowPacked(HlcState state) {
    return state.clock.nowPacked();
  }

  @Benchmark
  public long hlcUpdatePacked(HlcState state) {
    return state.clock.updatePacked(state.remotePacked);
  }

  @Benchmark
  public long walAppend(WalState state) {
    return state.wal.append(state.payload);
  }

  private static long pack(HlcTimestamp timestamp) {
    return USE_SOLUTION
        ? Kata23_HybridLogicalClockSolution.pack(timestamp)
        : Kata23_HybridLogicalClock.pack(timestamp);
  }
}
