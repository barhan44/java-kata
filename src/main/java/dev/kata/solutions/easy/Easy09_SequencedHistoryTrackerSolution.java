package dev.kata.solutions.easy;

import dev.kata.easy.Easy09_SequencedHistoryTracker;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Эталон Easy 09.
 *
 * <p>{@link LinkedHashSet} — одновременно {@code Set} и {@code SequencedSet}. {@code record}
 * сначала удаляет старое вхождение (если оно было), затем {@code addFirst} кладёт элемент в начало;
 * дубликат исчезает, а порядок «новое → старое» поддерживается без ручной перестановки. При
 * превышении лимита {@code removeLast} убирает самый старый просмотр.
 *
 * <p>{@code getRecent} копирует элементы через {@code List.copyOf}, поэтому вызывающий не может
 * изменить внутреннюю историю. Все операции — O(1) в среднем.
 */
public final class Easy09_SequencedHistoryTrackerSolution extends Easy09_SequencedHistoryTracker {

  @Override
  public <T> RecentHistory<T> newHistory(int maxSize) {
    return new LinkedHashSetHistory<>(maxSize);
  }

  private static final class LinkedHashSetHistory<T> extends RecentHistory<T> {

    private final LinkedHashSet<T> items = new LinkedHashSet<>();

    private LinkedHashSetHistory(int maxSize) {
      super(maxSize);
    }

    @Override
    public void record(T item) {
      Objects.requireNonNull(item, "item");
      items.remove(item);
      items.addFirst(item);
      if (items.size() > maxSize) {
        items.removeLast();
      }
    }

    @Override
    public List<T> getRecent() {
      return List.copyOf(items);
    }
  }
}
