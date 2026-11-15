package dev.kata.solutions.streams;

import dev.kata.streams.Kata18_CustomStreamGatherer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Эталонное решение Kata 18.
 *
 * <h2>Gatherer как push-трансформер</h2>
 *
 * <p>Состояние создаётся лениво, при первом {@code tryAdvance} результата. Затем spliterator тянет
 * элементы источника по одному и передаёт их в {@link StreamGatherer#integrate}. Gatherer может
 * вызвать {@code downstream.accept} ноль, один или несколько раз; результаты складываются в
 * переиспользуемый буфер. Как только в буфере появился элемент, {@code tryAdvance} отдаёт его
 * потребителю и возвращает {@code true}. Если источник исчерпан, вызывается {@code finish} — туда
 * попадают хвостовые окна/батчи.
 *
 * <h2>Почему Spliterator, а не Collector</h2>
 *
 * <p>{@code Collector} материализует весь результат и не умеет short-circuit. Spliterator —
 * единственный способ сохранить ленивость и остановиться по {@code limit(n)}, не вычитывая источник
 * до конца. {@link Spliterators.AbstractSpliterator} даёт готовую реализацию {@code estimateSize} и
 * {@code trySplit}; разбиение не поддерживается сознательно: состояние gatherer'а упорядочено, и
 * параллельное разбиение потребовало бы слияния состояний с сохранением порядка.
 *
 * <h2>Буфер и null</h2>
 *
 * <p>Буфер — {@link ArrayList}, а не {@code ArrayDeque}: стримы могут содержать {@code null}, а
 * {@code ArrayDeque} их запрещает. После опустошения список очищается, но сохраняет capacity,
 * поэтому на каждый выход объект не создаётся. Выходные списки окон/батчей — неизменяемые копии,
 * также допускающие {@code null}.
 *
 * <h2>Ленивость</h2>
 *
 * <p>{@code source.spliterator()} не запускает обработку сам по себе. Первый {@code tryAdvance}
 * тянет ровно столько элементов, сколько нужно для первого результата (для окна — {@code
 * windowSize}, для батча — до {@code maxSize} или срабатывания предиката). {@code limit(1)} после
 * gather не вычитает бесконечный источник.
 *
 * <h2>Закрытие</h2>
 *
 * <p>Результат создаётся через {@code StreamSupport.stream(...).onClose(source::close)}: закрытие
 * результата закрывает исходный стрим и освобождает его ресурсы.
 */
public final class Kata18_CustomStreamGathererSolution extends Kata18_CustomStreamGatherer {

  /**
   * Оборачивает стрим. Wildcard {@code ?} в типе состояния захватывается generic-методом {@code
   * gatherInternal}; это безопасно, потому что состояние создаёт сам gatherer и передаёт его только
   * своим же методам.
   */
  public static <T, R> Stream<R> gather(Stream<T> source, StreamGatherer<T, ?, R> gatherer) {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(gatherer, "gatherer");
    return gatherInternal(source, gatherer);
  }

  private static <T, A, R> Stream<R> gatherInternal(
      Stream<T> source, StreamGatherer<T, A, R> gatherer) {
    Spliterator<T> sourceSpliterator = source.spliterator();
    Spliterator<R> resultSpliterator = new GatheringSpliterator<>(sourceSpliterator, gatherer);
    return StreamSupport.stream(resultSpliterator, false).onClose(source::close);
  }

  /** Ленивая дедупликация по ключу. */
  public static <T, K> StreamGatherer<T, ?, T> distinctBy(
      Function<? super T, ? extends K> keyExtractor) {
    Objects.requireNonNull(keyExtractor, "keyExtractor");
    return new StreamGatherer<T, Set<K>, T>() {
      @Override
      public Set<K> initial() {
        return new HashSet<>();
      }

      @Override
      public boolean integrate(Set<K> seen, T element, Consumer<T> downstream) {
        if (seen.add(keyExtractor.apply(element))) {
          downstream.accept(element);
        }
        return true;
      }
    };
  }

  /** Скользящее окно с произвольным шагом. */
  public static <T> StreamGatherer<T, ?, List<T>> slidingWindow(int windowSize, int step) {
    if (windowSize <= 0) {
      throw new IllegalArgumentException("windowSize must be > 0, got " + windowSize);
    }
    if (step <= 0) {
      throw new IllegalArgumentException("step must be > 0, got " + step);
    }
    return new StreamGatherer<T, WindowState<T>, List<T>>() {
      @Override
      public WindowState<T> initial() {
        return new WindowState<>(windowSize, step);
      }

      @Override
      public boolean integrate(WindowState<T> state, T element, Consumer<List<T>> downstream) {
        state.add(element);
        if (state.isReady()) {
          downstream.accept(state.snapshot());
        }
        return true;
      }
    };
  }

  /** Батчи по размеру и предикату. */
  public static <T> StreamGatherer<T, ?, List<T>> batchWithCondition(
      int maxSize, BiPredicate<List<T>, T> shouldFlushBefore) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize must be > 0, got " + maxSize);
    }
    Objects.requireNonNull(shouldFlushBefore, "shouldFlushBefore");
    return new StreamGatherer<T, BatchState<T>, List<T>>() {
      @Override
      public BatchState<T> initial() {
        return new BatchState<>(maxSize, shouldFlushBefore);
      }

      @Override
      public boolean integrate(BatchState<T> state, T element, Consumer<List<T>> downstream) {
        state.add(element, downstream);
        return true;
      }

      @Override
      public void finish(BatchState<T> state, Consumer<List<T>> downstream) {
        state.flush(downstream);
      }
    };
  }

  /** Состояние скользящего окна: кольцевой буфер Object[] (допускает null). */
  private static final class WindowState<T> {
    private final Object[] elements;
    private final int windowSize;
    private final int step;
    private int head;
    private int size;
    private long count;

    WindowState(int windowSize, int step) {
      this.windowSize = windowSize;
      this.step = step;
      this.elements = new Object[windowSize];
    }

    void add(T element) {
      if (size < windowSize) {
        elements[(head + size) % windowSize] = element;
        size++;
      } else {
        elements[head] = element;
        head = (head + 1) % windowSize;
      }
      count++;
    }

    boolean isReady() {
      if (size < windowSize) {
        return false;
      }
      return (count - windowSize) % step == 0L;
    }

    List<T> snapshot() {
      List<T> copy = new ArrayList<>(windowSize);
      for (int i = 0; i < windowSize; i++) {
        copy.add(elementAt(i));
      }
      return Collections.unmodifiableList(copy);
    }

    @SuppressWarnings("unchecked")
    private T elementAt(int index) {
      return (T) elements[(head + index) % windowSize];
    }
  }

  /** Состояние батча. */
  private static final class BatchState<T> {
    private final int maxSize;
    private final BiPredicate<List<T>, T> shouldFlushBefore;
    private final List<T> batch = new ArrayList<>();
    private final List<T> view = Collections.unmodifiableList(batch);

    BatchState(int maxSize, BiPredicate<List<T>, T> shouldFlushBefore) {
      this.maxSize = maxSize;
      this.shouldFlushBefore = shouldFlushBefore;
    }

    void add(T element, Consumer<List<T>> downstream) {
      if (!batch.isEmpty() && (batch.size() >= maxSize || shouldFlushBefore.test(view, element))) {
        flush(downstream);
      }
      batch.add(element);
      if (batch.size() >= maxSize) {
        flush(downstream);
      }
    }

    void flush(Consumer<List<T>> downstream) {
      if (batch.isEmpty()) {
        return;
      }
      downstream.accept(Collections.unmodifiableList(new ArrayList<>(batch)));
      batch.clear();
    }
  }

  /**
   * Ленивый spliterator, который тянет источник по одному элементу и складывает выходы gatherer'а в
   * буфер.
   */
  private static final class GatheringSpliterator<T, A, R>
      extends Spliterators.AbstractSpliterator<R> {

    private final Spliterator<T> source;
    private final StreamGatherer<T, A, R> gatherer;
    private final List<R> buffer = new ArrayList<>();
    private final Consumer<R> downstream = buffer::add;
    private final Consumer<T> sourceConsumer = this::acceptElement;

    private A state;
    private boolean initialized;
    private boolean sourceExhausted;
    private boolean finished;
    private boolean stopped;
    private int head;

    GatheringSpliterator(Spliterator<T> source, StreamGatherer<T, A, R> gatherer) {
      super(
          Long.MAX_VALUE, source.hasCharacteristics(Spliterator.ORDERED) ? Spliterator.ORDERED : 0);
      this.source = source;
      this.gatherer = gatherer;
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
      Objects.requireNonNull(action, "action");
      if (!initialized) {
        state = gatherer.initial();
        initialized = true;
      }

      while (head >= buffer.size() && !finished) {
        if (!sourceExhausted) {
          boolean advanced = source.tryAdvance(sourceConsumer);
          if (!advanced) {
            sourceExhausted = true;
          }
          if (stopped) {
            finished = true;
          }
        } else {
          gatherer.finish(state, downstream);
          finished = true;
        }
      }

      if (head >= buffer.size()) {
        buffer.clear();
        head = 0;
        return false;
      }

      R value = buffer.get(head++);
      if (head == buffer.size()) {
        buffer.clear();
        head = 0;
      }
      action.accept(value);
      return true;
    }

    private void acceptElement(T element) {
      if (!gatherer.integrate(state, element, downstream)) {
        stopped = true;
      }
    }
  }
}
