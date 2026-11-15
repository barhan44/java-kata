package dev.kata.solutions.memory;

import dev.kata.memory.Kata06_OffHeapByteBufferPool;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Эталонное решение Kata 06.
 *
 * <h2>Почему direct-буферы и почему пул</h2>
 *
 * <p>{@link ByteBuffer#allocateDirect(int)} выделяет память вне heap. Она не двигается сборщиком
 * мусора, поэтому подходит для I/O без копирования. Цена — системный {@code malloc}, регистрация
 * Cleaner'а и учёт в {@code Bits.reserveMemory}. При десятках тысяч запросов в секунду эти
 * накладные расходы доминируют. Пул превращает «выделение/освобождение» в «взять из
 * очереди/положить обратно».
 *
 * <h2>Синхронизация</h2>
 *
 * <p>Очередь пула и флаг {@code closed} защищены одним коротким {@code synchronized}-блоком.
 * Критическая секция — только операции над {@link ArrayDeque} (O(1), без I/O). Это проще и
 * надёжнее, чем lock-free очередь с CAS-циклом, а конкуренция здесь несопоставима с временем работы
 * с самим буфером. Счётчики — атомарные, чтобы читать их без блокировки.
 *
 * <h2>Cleaner и защита от утечек</h2>
 *
 * <p>Каждый lease регистрирует действие в общем {@link Cleaner}. Ключевое требование: действие
 * <b>не должно</b> захватывать сам lease, иначе он никогда не станет unreachable. Поэтому действие
 * захватывает:
 *
 * <ul>
 *   <li>{@link LeaseState} — буфер + флаг «закрыт пользователем»;
 *   <li>ссылку на пул.
 * </ul>
 *
 * <p>При явном {@code close()} флаг выставляется через CAS, затем вызывается {@code
 * cleanable.clean()} — действие выполняется ровно один раз. Если же объект собрал GC, флаг остался
 * {@code false}: действие увеличивает {@link #leakCount()} и всё равно возвращает буфер в пул.
 * Утечка не фатальна, но сигнализирует об ошибке в коде вызывающего.
 *
 * <p>Cleaner работает в отдельном потоке и недетерминирован — это страховка, а не замена
 * try-with-resources.
 *
 * <h2>Двойной возврат</h2>
 *
 * <p>{@link AtomicBoolean} в состоянии гарантирует, что {@code clean()} выполнится один раз. Без
 * этого два потока могли бы положить один и тот же {@link ByteBuffer} в пул дважды, и два следующих
 * {@code acquire()} получили бы один объект — классическая порча данных.
 *
 * <h2>Закрытие пула</h2>
 *
 * <p>{@code close()} выставляет флаг и опустошает очередь: буферы становятся unreachable и
 * освобождаются собственным Cleaner'ом direct-буфера. Уже выданные lease можно закрыть — их буферы
 * будут отброшены, а не возвращены в закрытый пул.
 */
public final class Kata06_OffHeapByteBufferPoolSolution extends Kata06_OffHeapByteBufferPool {

  private static final Cleaner CLEANER = Cleaner.create();

  private final Object lock = new Object();
  private final Deque<ByteBuffer> pool = new ArrayDeque<>();
  private final AtomicInteger pooled = new AtomicInteger();
  private final AtomicInteger inUse = new AtomicInteger();
  private final AtomicLong leaked = new AtomicLong();
  private boolean closed;

  public Kata06_OffHeapByteBufferPoolSolution(int bufferSize, int maxPooledBuffers) {
    super(bufferSize, maxPooledBuffers);
    if (bufferSize <= 0) {
      throw new IllegalArgumentException("bufferSize must be > 0, got " + bufferSize);
    }
    if (maxPooledBuffers < 0) {
      throw new IllegalArgumentException("maxPooledBuffers must be >= 0, got " + maxPooledBuffers);
    }
  }

  @Override
  public Lease acquire() {
    ByteBuffer buffer;
    synchronized (lock) {
      if (closed) {
        throw new IllegalStateException("pool is closed");
      }
      buffer = pool.pollFirst();
      if (buffer != null) {
        pooled.decrementAndGet();
        buffer.clear();
      }
    }

    if (buffer == null) {
      buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    inUse.incrementAndGet();
    return new LeaseImpl(buffer);
  }

  @Override
  public int pooledCount() {
    return pooled.get();
  }

  @Override
  public int inUseCount() {
    return inUse.get();
  }

  @Override
  public long leakCount() {
    return leaked.get();
  }

  @Override
  public void close() {
    synchronized (lock) {
      if (closed) {
        return;
      }
      closed = true;
      while (!pool.isEmpty()) {
        pool.pollFirst();
        pooled.decrementAndGet();
      }
    }
  }

  /**
   * Возвращает буфер в пул или отбрасывает его, если пул закрыт/переполнен. Вызывается ровно один
   * раз на lease (гарантируется CAS в {@link LeaseState}).
   */
  private void recycle(ByteBuffer buffer) {
    synchronized (lock) {
      if (!closed && pooled.get() < maxPooledBuffers) {
        buffer.clear();
        pool.addFirst(buffer);
        pooled.incrementAndGet();
      }
      // Иначе буфер становится unreachable; его освободит Cleaner самого ByteBuffer.
    }
  }

  /**
   * Состояние lease, на которое ссылаются и сам lease, и действие Cleaner'а. Не содержит ссылки на
   * lease — иначе Cleaner никогда бы не сработал.
   */
  private static final class LeaseState {
    private final ByteBuffer buffer;
    private final AtomicBoolean closed = new AtomicBoolean();

    private LeaseState(ByteBuffer buffer) {
      this.buffer = buffer;
    }
  }

  /** Действие Cleaner'а: декремент inUse, учёт утечки, возврат буфера. */
  private static final class Releaser implements Runnable {
    private final Kata06_OffHeapByteBufferPoolSolution owner;
    private final LeaseState state;

    private Releaser(Kata06_OffHeapByteBufferPoolSolution owner, LeaseState state) {
      this.owner = owner;
      this.state = state;
    }

    @Override
    public void run() {
      owner.inUse.decrementAndGet();
      if (!state.closed.get()) {
        // Lease собрали без close(): это утечка с точки зрения вызывающего кода.
        owner.leaked.incrementAndGet();
      }
      owner.recycle(state.buffer);
    }
  }

  /** Обёртка над буфером. Держит пул через внешний класс. */
  private final class LeaseImpl implements Lease {
    private final LeaseState state;
    private final Cleaner.Cleanable cleanable;

    private LeaseImpl(ByteBuffer buffer) {
      this.state = new LeaseState(buffer);
      this.cleanable =
          CLEANER.register(this, new Releaser(Kata06_OffHeapByteBufferPoolSolution.this, state));
    }

    @Override
    public ByteBuffer buffer() {
      if (state.closed.get()) {
        throw new IllegalStateException("lease is already closed");
      }
      return state.buffer;
    }

    @Override
    public void close() {
      if (state.closed.compareAndSet(false, true)) {
        cleanable.clean();
      }
    }
  }
}
