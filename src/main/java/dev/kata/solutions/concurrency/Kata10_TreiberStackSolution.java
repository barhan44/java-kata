package dev.kata.solutions.concurrency;

import dev.kata.concurrency.Kata10_TreiberStack;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Эталонное решение Kata 10.
 *
 * <h2>Алгоритм Трайбера</h2>
 *
 * <p>Вершина стека — пара «ссылка на узел + штамп». {@code push} создаёт новый узел, читает вершину
 * и пытается заменить её CAS-ом; {@code pop} читает вершину и пытается заменить её на {@code
 * top.next}. При неудаче операция повторяется. Это классический lock-free алгоритм: система в целом
 * продвигается вперёд, даже если один поток остановлен между чтением и CAS.
 *
 * <h2>Зачем штамп и как он читается</h2>
 *
 * <p>{@link AtomicStampedReference#get(int[])} возвращает ссылку и штамп <b>атомарно</b>. Нельзя
 * делать {@code getReference()} и {@code getStamp()} отдельно: между вызовами другой поток может
 * изменить и ссылку, и штамп, и мы получим «ссылку из одного состояния, штамп из другого». Именно
 * такая рассинхронизация ломает ABA-защиту.
 *
 * <p>Массив для штампа — это выходной параметр API. Чтобы не аллоцировать {@code int[1]} на каждую
 * операцию, используется {@link ThreadLocal}; он безопасен, потому что буфер не выходит за пределы
 * текущего потока.
 *
 * <h2>Почему штамп нужен, если узлы не переиспользуются</h2>
 *
 * <p>В этой реализации {@code push} всегда создаёт новый {@link Node}, а удалённый узел никогда не
 * возвращается в стек. Поэтому ABA по идентичности ссылки здесь невозможен: новый объект — это
 * всегда новая ссылка, даже если GC разместит его по тому же адресу (старый объект не может быть
 * собран, пока на него ссылается читатель). Штамп тем не менее обязателен по контракту задачи и
 * делает реализацию корректной при добавлении пула узлов: если {@code pop} начнёт складывать узлы в
 * free-list, а {@code push} — брать их оттуда, один и тот же {@code Node} будет возвращаться в
 * стек, и ABA без версии воспроизведётся стабильно.
 *
 * <h2>Память и публикация</h2>
 *
 * <p>Поле {@code Node.next} не {@code final}, но записывается до CAS. Запись в CAS — это volatile
 * write, а успешный {@code get} другого потока — volatile read; между ними возникает
 * happens-before, поэтому читатель гарантированно увидит корректный {@code next} и {@code item}.
 * После успешного {@code pop} ссылка {@code current.next} обнуляется: это не обязательно для
 * корректности, но помогает GC не удерживать длинную цепочку уже удалённых узлов.
 *
 * <h2>Счётчик размера</h2>
 *
 * <p>{@link LongAdder} дешевле {@code AtomicLong} при высокой конкуренции: инкременты расходятся по
 * ячейкам и суммируются только при чтении. {@code size()} — приблизительный снимок (сумма ячеек не
 * атомарна), что явно оговорено в контракте. Точное значение доступно после завершения операций.
 */
public final class Kata10_TreiberStackSolution<T> extends Kata10_TreiberStack<T> {

  /** Переиспользуемый буфер для атомарного чтения пары (ссылка, штамп). */
  private static final ThreadLocal<int[]> STAMP_HOLDER = ThreadLocal.withInitial(() -> new int[1]);

  private final AtomicStampedReference<Node<T>> top = new AtomicStampedReference<>(null, 0);
  private final LongAdder size = new LongAdder();

  @Override
  public void push(T item) {
    Objects.requireNonNull(item, "item");
    Node<T> node = new Node<>(item);
    int[] stamp = STAMP_HOLDER.get();

    while (true) {
      Node<T> current = top.get(stamp);
      node.next = current;
      if (top.compareAndSet(current, node, stamp[0], stamp[0] + 1)) {
        size.increment();
        return;
      }
      // CAS провалился: другой поток изменил вершину или штамп. Перечитываем пару и повторяем.
    }
  }

  @Override
  public Optional<T> pop() {
    int[] stamp = STAMP_HOLDER.get();

    while (true) {
      Node<T> current = top.get(stamp);
      if (current == null) {
        return Optional.empty();
      }

      Node<T> next = current.next;
      if (top.compareAndSet(current, next, stamp[0], stamp[0] + 1)) {
        size.decrement();
        current.next = null; // help GC: удалённый узел больше не держит остаток стека
        return Optional.of(current.item);
      }
      // Штамп изменился или вершина заменена — повторяем с новым снимком.
    }
  }

  @Override
  public Optional<T> peek() {
    Node<T> current = top.getReference();
    return current == null ? Optional.empty() : Optional.of(current.item);
  }

  @Override
  public boolean isEmpty() {
    return top.getReference() == null;
  }

  @Override
  public int size() {
    long current = size.sum();
    if (current <= 0) {
      return 0;
    }
    return current > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) current;
  }

  /**
   * Узел стека. {@code item} неизменяем; {@code next} переписывается при повторных попытках CAS до
   * успешной публикации.
   */
  private static final class Node<T> {
    private final T item;
    private Node<T> next;

    Node(T item) {
      this.item = item;
    }
  }
}
