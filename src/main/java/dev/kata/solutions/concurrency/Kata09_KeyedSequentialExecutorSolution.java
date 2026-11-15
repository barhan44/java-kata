package dev.kata.solutions.concurrency;

import dev.kata.concurrency.Kata09_KeyedSequentialExecutor;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Эталонное решение Kata 09.
 *
 * <h2>Модель: mailbox на каждый ключ</h2>
 *
 * <p>Карта {@code K -> KeyQueue} хранит только ключи, у которых есть работа. Каждая очередь — это
 * {@link ArrayDeque} задач и флаг {@code draining}. Первый {@code submit} для ключа создаёт очередь
 * и запускает на виртуальном потоке цикл {@code drain}; цикл последовательно забирает задачи из
 * головы и выполняет их. Пока очередь не опустеет, поток не завершается. Разные ключи получают
 * разные виртуальные потоки и работают параллельно.
 *
 * <p>Виртуальный поток здесь идеален: если задача блокируется на I/O, поток паркуется и не занимает
 * поток ОС. Держать по виртуальному потоку на «активный» ключ дёшево; поток живёт только пока в
 * очереди есть задачи.
 *
 * <h2>Почему {@code compute}, а не {@code computeIfAbsent} + {@code queue.add}</h2>
 *
 * <p>Добавление задачи и удаление опустевшей очереди обязаны быть атомарными относительно карты.
 * Иначе возникает окно, в котором {@code submit} уже получил ссылку на очередь, а drain успел
 * удалить её из карты; задача попадает в «осиротевшую» очередь и либо теряется, либо выполняется
 * параллельно с задачей из новой очереди того же ключа.
 *
 * <p>В решении обе операции идут через {@link ConcurrentHashMap#compute}:
 *
 * <ul>
 *   <li><b>submit:</b> {@code compute(key, (k, q) -> { q.enqueue(task); return q; })} — задача
 *       добавляется под биновой блокировкой карты.
 *   <li><b>очистка:</b> {@code compute(key, (k, q) -> q == this && idle ? null : q)} — удаление
 *       происходит только если в карте всё ещё лежит именно эта очередь и она пуста.
 * </ul>
 *
 * <p>Обе функции выполняются под одной и той же блокировкой бакета, поэтому они линеаризуются: либо
 * enqueue видит очередь до удаления (и она перестаёт быть пустой — удаление отменится), либо
 * удаление происходит раньше, и следующий submit создаёт новую очередь. Третьего варианта нет.
 *
 * <h2>Порядок блокировок</h2>
 *
 * <p>Допустим только порядок «бакет карты → монитор очереди»: {@code compute} вызывает {@code
 * enqueue}/{@code isIdle}, которые синхронизируются на очереди. Drain никогда не вызывает {@code
 * compute}, удерживая монитор очереди: он отпускает монитор, затем пытается удалиться. Поэтому
 * циклического ожидания нет.
 *
 * <h2>Обработка ошибок и закрытие</h2>
 *
 * <p>Задача выполняется в {@code try/catch(Throwable)}: future завершается исключительно, а drain
 * продолжает цикл. Следующая задача ключа не страдает от предыдущей.
 *
 * <p>{@link #close()} выставляет {@code closed}, вызывает {@code ExecutorService.close()} (shutdown
 * + ожидание уже принятых задач) и очищает карту. {@code submit} после закрытия возвращает
 * исключительно завершённый future, а не бросает синхронно — это удобнее для асинхронного
 * вызывающего кода.
 */
public final class Kata09_KeyedSequentialExecutorSolution<K>
    extends Kata09_KeyedSequentialExecutor<K> {

  private final ConcurrentHashMap<K, KeyQueue> queues = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private final AtomicBoolean closed = new AtomicBoolean();

  @Override
  public <T> CompletableFuture<T> submit(K key, Callable<T> task) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(task, "task");

    if (closed.get()) {
      return rejected(new RejectedExecutionException("executor is closed"));
    }

    CompletableFuture<T> result = new CompletableFuture<>();
    Task<T> submitted = new Task<>(task, result);
    try {
      queues.compute(
          key,
          (k, existing) -> {
            KeyQueue queue = existing == null ? new KeyQueue(k) : existing;
            queue.enqueue(submitted);
            return queue;
          });
    } catch (RejectedExecutionException e) {
      // Гонка с close: задача не встала в очередь. Убираем опустевшую очередь, чтобы не
      // оставить мусор в карте, и возвращаем ошибку через future.
      queues.computeIfPresent(key, (k, queue) -> queue.isIdle() ? null : queue);
      result.completeExceptionally(e);
    }
    return result;
  }

  @Override
  public int activeKeyCount() {
    return queues.size();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      executor.close();
      queues.clear();
    }
  }

  private static <T> CompletableFuture<T> rejected(Throwable failure) {
    CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(failure);
    return future;
  }

  /**
   * Очередь одного ключа.
   *
   * <p>Нестатический inner-класс: ему нужны {@code queues} и {@code executor} внешнего объекта.
   * После удаления из карты очередь становится недостижимой и собирается GC вместе с задачами.
   */
  private final class KeyQueue {

    private final K key;

    /** Задачи в порядке поступления. Все обращения — под монитором {@code this}. */
    private final ArrayDeque<Task<?>> tasks = new ArrayDeque<>();

    /** true, если на executor-е уже запланирован (или выполняется) drain. */
    private boolean draining;

    KeyQueue(K key) {
      this.key = key;
    }

    /**
     * Добавляет задачу и при необходимости запускает drain.
     *
     * <p>Вызывается из {@code ConcurrentHashMap.compute}, то есть под блокировкой бакета карты.
     * {@code Task.enqueued} защищает от повторного добавления, если реализация карты когда-нибудь
     * повторит remapping-функцию.
     */
    synchronized void enqueue(Task<?> task) {
      boolean addedHere = false;
      if (!task.enqueued) {
        task.enqueued = true;
        tasks.addLast(task);
        addedHere = true;
      }

      if (draining) {
        return;
      }
      draining = true;
      try {
        executor.execute(this::drain);
      } catch (RejectedExecutionException e) {
        draining = false;
        if (addedHere) {
          tasks.removeLastOccurrence(task);
          task.enqueued = false;
        }
        throw e;
      }
    }

    /** Возвращает true, если в очереди нет задач и drain не запущен/не запланирован. */
    synchronized boolean isIdle() {
      return tasks.isEmpty() && !draining;
    }

    /**
     * Разбирает очередь до пустоты.
     *
     * <p>Сначала забирает задачу под монитором, затем выполняет её без блокировки. Пока очередь
     * пополняется, поток продолжает работать; когда задача кончилась — пытается атомарно удалить
     * очередь из карты.
     */
    private void drain() {
      while (true) {
        Task<?> task;
        synchronized (this) {
          task = tasks.pollFirst();
          if (task == null) {
            draining = false;
          }
        }

        if (task != null) {
          task.execute();
          continue;
        }

        if (removeIfIdle()) {
          return;
        }

        synchronized (this) {
          if (!tasks.isEmpty()) {
            if (draining) {
              // Пока мы пытались удалиться, enqueue добавил задачу и уже запланировал новый
              // drain. Уступаем ему, чтобы не запускать второй параллельный обход.
              return;
            }
            draining = true;
            continue;
          }
          if (draining) {
            // enqueue выставил draining и запланировал drain, но задачу уже успел забрать
            // другой (текущий) обход. Новый drain сам разберётся с пустой очередью.
            return;
          }
          if (queues.get(key) != this) {
            // Нас уже заменили новой очередью; в этой задач нет — можно завершаться.
            return;
          }
        }
        // Очередь пуста, не draining и всё ещё привязана к ключу — повторяем попытку удаления.
      }
    }

    /**
     * Атомарно удаляет очередь из карты, если она всё ещё привязана к ключу и пуста.
     *
     * @return true — запись удалена, drain может завершаться
     */
    private boolean removeIfIdle() {
      boolean[] removed = {false};
      queues.compute(
          key,
          (k, queue) -> {
            if (queue == this && isIdle()) {
              removed[0] = true;
              return null;
            }
            return queue;
          });
      return removed[0];
    }
  }

  /** Задача и future, который нужно завершить её результатом. */
  private static final class Task<T> {
    private final Callable<T> callable;
    private final CompletableFuture<T> result;

    /** Защищено монитором KeyQueue. */
    private boolean enqueued;

    Task(Callable<T> callable, CompletableFuture<T> result) {
      this.callable = callable;
      this.result = result;
    }

    void execute() {
      try {
        result.complete(callable.call());
      } catch (Throwable failure) {
        // Ошибка одной задачи не прерывает drain: следующая задача ключа выполнится.
        result.completeExceptionally(failure);
      }
    }
  }
}
