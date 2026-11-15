package dev.kata.solutions.distributed;

import dev.kata.distributed.Kata24_OrSetCrdt;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Эталонное решение Kata 24.
 *
 * <h2>Состояние и теги</h2>
 *
 * <p>Каждое добавление получает тег {@code (replicaId, sequence)}. Sequence локальный и монотонный;
 * разные реплики имеют разные id, поэтому теги глобально уникальны. Состояние:
 *
 * <pre>
 *   adds:    Map&lt;T, Set&lt;Tag&gt;&gt;   // все когда-либо добавленные теги элемента
 *   removed: Set&lt;Tag&gt;             // теги, которые эта реплика наблюдала при remove
 * </pre>
 *
 * <p>{@code remove(x)} не удаляет сам ключ из {@code adds}: он добавляет все наблюдаемые теги в
 * {@code removed}. Так состояние остаётся монотонным (только растёт), а merge — обычное объединение
 * множеств. Именно монотонность даёт идемпотентность и ассоциативность.
 *
 * <h2>Add-Wins</h2>
 *
 * <p>Свежий тег, созданный на другой реплике, отсутствует в {@code removed} этой реплики. После
 * merge он остаётся живым, даже если все старые теги элемента были удалены. Поэтому конкурентное
 * добавление побеждает удаление. Если обе реплики успели увидеть один и тот же тег и удалить его,
 * тег попадёт в {@code removed} обеих — элемент исчезнет.
 *
 * <h2>Слияние без deadlock</h2>
 *
 * <p>Метод {@code merge} не удерживает одновременно два монитора. Сначала под замком {@code other}
 * снимается копия его состояния, затем копия объединяется с {@code this} под замком {@code this}.
 * Поэтому взаимные {@code A.merge(B)} и {@code B.merge(A)} не могут заклинить друг друга. Слияние
 * видит согласованный снимок; конкурентное добавление, не попавшее в снимок, будет получено при
 * следующем merge — это нормально для CRDT.
 *
 * <h2>Сложность</h2>
 *
 * <ul>
 *   <li>{@code add}/{@code contains} — O(1) в среднем; {@code remove} — O(числа тегов элемента).
 *   <li>{@code elements} — O(суммарного числа тегов).
 *   <li>{@code merge} — O(размера снимка other + размера this).
 * </ul>
 */
public final class Kata24_OrSetCrdtSolution<T> extends Kata24_OrSetCrdt<T> {

  private final Map<T, Set<Tag>> adds = new HashMap<>();
  private final Set<Tag> removed = new HashSet<>();
  private long nextSequence;

  public Kata24_OrSetCrdtSolution(String replicaId) {
    super(replicaId);
    Objects.requireNonNull(replicaId, "replicaId");
    if (replicaId.isEmpty()) {
      throw new IllegalArgumentException("replicaId must not be empty");
    }
  }

  @Override
  public synchronized void add(T element) {
    Tag tag = new Tag(replicaId, ++nextSequence);
    adds.computeIfAbsent(element, ignored -> new HashSet<>()).add(tag);
  }

  @Override
  public synchronized void remove(T element) {
    Set<Tag> tags = adds.get(element);
    if (tags != null) {
      removed.addAll(tags);
    }
  }

  @Override
  public synchronized boolean contains(T element) {
    Set<Tag> tags = adds.get(element);
    if (tags == null) {
      return false;
    }
    for (Tag tag : tags) {
      if (!removed.contains(tag)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized Set<T> elements() {
    Set<T> visible = new HashSet<>();
    for (Map.Entry<T, Set<Tag>> entry : adds.entrySet()) {
      for (Tag tag : entry.getValue()) {
        if (!removed.contains(tag)) {
          visible.add(entry.getKey());
          break;
        }
      }
    }
    return Collections.unmodifiableSet(visible);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void merge(Kata24_OrSetCrdt<T> other) {
    Objects.requireNonNull(other, "other");
    if (other == this) {
      return;
    }
    if (!(other instanceof Kata24_OrSetCrdtSolution<?> otherSolution)) {
      throw new IllegalArgumentException(
          "merge requires another Kata24_OrSetCrdtSolution instance");
    }
    Kata24_OrSetCrdtSolution<T> typed = (Kata24_OrSetCrdtSolution<T>) otherSolution;

    Map<T, Set<Tag>> otherAdds = new HashMap<>();
    Set<Tag> otherRemoved;
    synchronized (typed) {
      for (Map.Entry<T, Set<Tag>> entry : typed.adds.entrySet()) {
        otherAdds.put(entry.getKey(), new HashSet<>(entry.getValue()));
      }
      otherRemoved = new HashSet<>(typed.removed);
    }

    synchronized (this) {
      for (Map.Entry<T, Set<Tag>> entry : otherAdds.entrySet()) {
        adds.computeIfAbsent(entry.getKey(), ignored -> new HashSet<>()).addAll(entry.getValue());
      }
      removed.addAll(otherRemoved);
    }
  }

  /** Уникальный тег одного добавления. */
  private record Tag(String replicaId, long sequence) {
    Tag {
      Objects.requireNonNull(replicaId, "replicaId");
      if (sequence <= 0L) {
        throw new IllegalArgumentException("sequence must be > 0, got " + sequence);
      }
    }
  }
}
