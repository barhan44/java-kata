package dev.kata.benchmarks;

import dev.kata.memory.Kata14_ArenaSlabAllocator;
import dev.kata.solutions.memory.Kata14_ArenaSlabAllocatorSolution;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Бенчмарк Kata 14: off-heap slab allocate/free против выделения массива в куче.
 *
 * <h2>Сценарий</h2>
 *
 * <p>Каждый поток получает собственный {@link Kata14_ArenaSlabAllocator} ({@code Scope.Thread}),
 * чтобы измерять стоимость самого аллокатора, а не конкуренцию за его lock. Сравниваются:
 *
 * <ul>
 *   <li>{@code slabAllocateFree} — {@code allocate()} + {@code free()}. Слот возвращается в
 *       intrusive free-list; Java-объект не создаётся.
 *   <li>{@code heapArray} — {@code new byte[64]} на каждую операцию. Массив уходит в JMH blackhole,
 *       поэтому escape analysis его не устраняет; GC обязан его собрать.
 *   <li>{@code heapLongArray} — {@code new long[8]} на каждую операцию: тот же объём, другой
 *       профиль заголовка и выравнивания.
 * </ul>
 *
 * <p>Версия с записью в слот намеренно не включена: {@link Kata14_ArenaSlabAllocator#slice(long)}
 * создаёт Java-обёртку {@link MemorySegment}, и её аллокация замаскировала бы главный вывод —
 * {@code allocate}/{@code free} не создают мусора.
 *
 * <h2>Что смотреть в результатах</h2>
 *
 * <p>{@code BenchmarkRunner} подключает {@code GCProfiler}. У {@code slabAllocateFree} метрика
 * {@code gc.alloc.rate.norm} должна быть около нуля: аллокатор пишет в off-heap сегмент и не
 * создаёт мусора. У {@code heapArray} она будет не меньше 64 B/op (заголовок массива +
 * выравнивание), а {@code gc.count} покажет, что сборщик действительно работает. Именно это и есть
 * цена «обычного» выделения на длинной дистанции.
 *
 * <p>Скорость slab-операции может быть выше или ниже одного {@code new byte[64]} в зависимости от
 * JIT и lock-а: задача бенчмарка — показать разницу в <b>аллокациях</b>, а не объявить победителя
 * по latency. Для сравнения lock-free slab-аллокатора уберите {@code ReentrantLock} и используйте
 * CAS-версию.
 *
 * <h2>Аннотации</h2>
 *
 * <ul>
 *   <li>{@link State}{@code (Scope.Thread)} — свой аллокатор и счётчик на поток; нет ложного
 *       sharing-а и борьбы за общий lock.
 *   <li>{@link Setup}{@code (Level.Trial)} — арена создаётся один раз на поток, вне измерений.
 *   <li>{@link TearDown}{@code (Level.Trial)} — арена закрывается после прогона.
 *   <li>{@link Threads}{@code (4)} — четыре независимых потока; каждый со своим сегментом.
 * </ul>
 *
 * <h2>Запуск</h2>
 *
 * <pre>
 * mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
 *     -Dexec.args="SlabAllocator" -DuseSolution=true
 * </pre>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@Threads(4)
public class SlabAllocatorBenchmark {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final int SLOT_SIZE = 64;
  private static final int SLOT_COUNT = 1024;

  /** Свой аллокатор на каждый поток. */
  @State(Scope.Thread)
  public static class AllocatorState {
    Kata14_ArenaSlabAllocator allocator;

    @Setup(Level.Trial)
    public void setUp() {
      allocator =
          USE_SOLUTION
              ? new Kata14_ArenaSlabAllocatorSolution(SLOT_SIZE, SLOT_COUNT)
              : new Kata14_ArenaSlabAllocator(SLOT_SIZE, SLOT_COUNT);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
      allocator.close();
    }
  }

  /** Off-heap: allocate -> write -> free. */
  @Benchmark
  public long slabAllocateFree(AllocatorState state) {
    long slot = state.allocator.allocate();
    if (slot < 0) {
      throw new IllegalStateException("allocator is empty");
    }
    state.allocator.free(slot);
    return slot;
  }

  /** Heap: новый массив на каждую операцию. */
  @Benchmark
  public byte[] heapArray() {
    return new byte[SLOT_SIZE];
  }

  /** Heap: массив long на каждую операцию (другой профиль заголовка/выравнивания). */
  @Benchmark
  public long[] heapLongArray() {
    return new long[SLOT_SIZE / Long.BYTES];
  }
}
