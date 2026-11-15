package dev.kata.easy;

import java.util.List;

/**
 * Easy 09. История просмотров на Java 21 SequencedSet.
 *
 * <h2>Задача</h2>
 *
 * <p>{@link RecentHistory} хранит не более {@code maxSize} последних уникальных элементов. {@link
 * RecentHistory#record(Object)} делает элемент самым новым: если он уже был, то поднимается наверх
 * без дубликата; при переполнении вытесняется самый старый. {@link RecentHistory#getRecent()}
 * возвращает элементы от нового к старому.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * history = newHistory(3)
 * record("a"); record("b"); record("c")
 * getRecent() → ["c","b","a"]
 * record("a")
 * getRecent() → ["a","c","b"]
 * record("d")
 * getRecent() → ["d","a","c"]   // "b" вытеснен
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>{@code maxSize > 0}; элементы не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Все операции O(1) в среднем благодаря {@link java.util.LinkedHashSet}, который в Java 21
 * реализует {@link java.util.SequencedSet}: {@code addFirst} перемещает элемент в начало, а {@code
 * removeLast} удаляет самый старый. Ручной список с поиском дал бы O(n) на {@code record}. Снимок
 * истории неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy09_SequencedHistoryTrackerSolution}.
 */
public class Easy09_SequencedHistoryTracker {

  /** Ограниченная история уникальных элементов, упорядоченная от нового к старому. */
  public static class RecentHistory<T> {

    protected final int maxSize;

    protected RecentHistory(int maxSize) {
      if (maxSize <= 0) {
        throw new IllegalArgumentException("maxSize must be > 0, got " + maxSize);
      }
      this.maxSize = maxSize;
    }

    /** Записывает просмотр, поднимая существующий элемент наверх. */
    public void record(T item) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Возвращает снимок истории от самого нового к самому старому. */
    public List<T> getRecent() {
      throw new UnsupportedOperationException("Not implemented yet");
    }
  }

  /**
   * Фабрика истории. Тесты вызывают её, чтобы выбрать заготовку или эталон; в эталоне возвращается
   * реализация на {@code LinkedHashSet} с sequenced-операциями.
   */
  public <T> RecentHistory<T> newHistory(int maxSize) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
