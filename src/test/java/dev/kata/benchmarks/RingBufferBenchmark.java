package dev.kata.benchmarks;

import dev.kata.algorithms.Kata04_RingBuffer;
import dev.kata.solutions.algorithms.Kata04_RingBufferSolution;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Бенчмарк Kata 04: SPSC ring buffer против {@link ArrayBlockingQueue}.
 *
 * <h2>Что и зачем измеряем</h2>
 *
 * <p>Сравниваем передачу событий между одним producer'ом и одним consumer'ом. Для честного
 * сравнения обе реализации работают в одинаковой конфигурации: один поток пишет, один читает,
 * ёмкость 1024. {@link ArrayBlockingQueue} — эталон «из коробки»: внутри {@code ReentrantLock} +
 * {@code Condition}, producer паркуется при полной очереди, consumer — при пустой. Наш ring buffer
 * крутится в spin-loop и не паркует потоки.
 *
 * <h2>Почему группы JMH</h2>
 *
 * <p>Обычный {@code @Benchmark} исполняется в одном потоке, а SPSC-структура раскрывается только
 * при параллельной работе producer'а и consumer'а. {@link Group} объединяет методы в одну
 * конкурентную «сцену»: JMH запускает оба метода одновременно и считает агрегированную пропускную
 * способность группы. {@link GroupThreads}{@code (1)} фиксирует ровно один поток на роль — это
 * важно, потому что ring buffer не поддерживает нескольких producers.
 *
 * <p>У класса два независимых состояния (ring buffer и ArrayBlockingQueue), поэтому {@link
 * State}{@code (Scope.Group)} даёт каждому состоянию свою пару потоков. Даже если JMH
 * переиспользует один state-объект, группы не пересекаются по данным: одна использует {@code ring},
 * другая — {@code queue}, а {@link AtomicLong} безопасен для обеих.
 *
 * <h2>Разбор аннотаций JMH</h2>
 *
 * <ul>
 *   <li>{@link BenchmarkMode}{@code (Mode.Throughput)} — событий в секунду. Для канала передачи
 *       данных это основная метрика; задержку одного события лучше мерить отдельным
 *       latency-бенчмарком с {@code Mode.SampleTime}.
 *   <li>{@link OutputTimeUnit}{@code (TimeUnit.SECONDS)} — ops/sec читаются интуитивно.
 *   <li>{@link Warmup}/{@link Measurement}/{@link Fork} — учебный профиль: 2×1 с прогрева, 3×1 с
 *       измерения, один форк. Для «продакшн»-выводов увеличьте до 5–10 итераций и 3 форков.
 *   <li>{@link Blackhole} — «потребитель» результата. Без него JIT может выбросить {@code
 *       poll()}/{@code take()}, если значение нигде не используется, и бенчмарк будет мерить
 *       пустоту.
 *   <li>{@link Setup}{@code (Level.Iteration)} — чистые структуры перед каждой итерацией.
 * </ul>
 *
 * <h2>О чём важно помнить при интерпретации</h2>
 *
 * <ul>
 *   <li>Spin-loop сжигает CPU. На машине с меньшим числом ядер, чем потоков JMH, результаты будут
 *       искажены планировщиком ОС.
 *   <li>JMH останавливает group-потоки прерыванием, поэтому циклы проверяют {@code isInterrupted()}
 *       и корректно выходят; иначе прогон мог бы зависнуть.
 *   <li>Проверка {@code Thread.onSpinWait()} — подсказка процессору (PAUSE на x86), она снижает
 *       энергопотребление и давление на hyper-threading, но не является барьером памяти.
 *   <li>Ёмкость 1024 специально велика: очереди редко заполняются, и мы меряем «горячий» путь, а не
 *       park/unpark. Для сценария backpressure уменьшите ёмкость.
 * </ul>
 *
 * <h2>Запуск</h2>
 *
 * <pre>
 * mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
 *     -Dexec.args="RingBuffer" -DuseSolution=true
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Group)
public class RingBufferBenchmark {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");
  private static final int CAPACITY = 1024;

  private Kata04_RingBuffer<Long> ring;
  private ArrayBlockingQueue<Long> queue;
  private final AtomicLong sequence = new AtomicLong();

  @Setup(Level.Iteration)
  public void setUp() {
    ring =
        USE_SOLUTION
            ? new Kata04_RingBufferSolution<>(CAPACITY)
            : new Kata04_RingBuffer<>(CAPACITY);
    queue = new ArrayBlockingQueue<>(CAPACITY);
  }

  // ------------------------------------------------------------------
  // Группа 1: наш lock-free SPSC ring buffer
  // ------------------------------------------------------------------

  @Benchmark
  @Group("ringBuffer")
  @GroupThreads(1)
  public void ringProducer() {
    long value = sequence.getAndIncrement();
    while (!ring.offer(value)) {
      if (Thread.currentThread().isInterrupted()) {
        return;
      }
      Thread.onSpinWait();
    }
  }

  @Benchmark
  @Group("ringBuffer")
  @GroupThreads(1)
  public void ringConsumer(Blackhole blackhole) {
    Long value;
    while ((value = ring.poll()) == null) {
      if (Thread.currentThread().isInterrupted()) {
        return;
      }
      Thread.onSpinWait();
    }
    blackhole.consume(value);
  }

  // ------------------------------------------------------------------
  // Группа 2: эталонная ArrayBlockingQueue
  // ------------------------------------------------------------------

  @Benchmark
  @Group("arrayBlockingQueue")
  @GroupThreads(1)
  public void queueProducer() {
    try {
      queue.put(sequence.getAndIncrement());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Benchmark
  @Group("arrayBlockingQueue")
  @GroupThreads(1)
  public void queueConsumer(Blackhole blackhole) {
    try {
      blackhole.consume(queue.take());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
