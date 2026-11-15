package dev.kata.easy;

import java.util.List;
import java.util.Optional;

/**
 * Easy 07. LRU-кэш на LinkedHashMap.
 *
 * <h2>Задача</h2>
 *
 * <p>Реализовать LRU-кэш фиксированной ёмкости: {@link LruCache#get(Object)} возвращает значение и
 * делает запись самой недавно использованной, {@link LruCache#put(Object, Object)} добавляет или
 * обновляет запись, {@link LruCache#size()} отдаёт текущий размер, а {@link
 * LruCache#keysInAccessOrder()} — ключи от самого старого обращения к самому новому. При
 * переполнении вытесняется запись, к которой дольше всего не обращались.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * cache = newLruCache(2)
 * put("a",1); put("b",2)
 * keysInAccessOrder() → ["a","b"]
 * get("a")            → Optional[1], порядок → ["b","a"]
 * put("c",3)          → вытеснен "b", порядок → ["a","c"]
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>{@code capacity > 0};
 *   <li>ключи и значения не {@code null}.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Все операции O(1) в среднем: {@code LinkedHashMap} с {@code accessOrder = true} поддерживает
 * двусвязный список порядка обращений, а {@code removeEldestEntry} избавляет от ручного учёта
 * вытеснения. Эталон не синхронизирован — это однопоточная структура; для конкурентного кэша нужен
 * отдельный слой блокировок.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy07_LruCacheWithLinkedHashMapSolution}.
 */
public class Easy07_LruCacheWithLinkedHashMap {

  /** LRU-кэш. Методы возвращают/принимают ненулевые ключи и значения. */
  public static class LruCache<K, V> {

    protected final int capacity;

    protected LruCache(int capacity) {
      if (capacity <= 0) {
        throw new IllegalArgumentException("capacity must be > 0, got " + capacity);
      }
      this.capacity = capacity;
    }

    /** Возвращает значение и помечает ключ как недавно использованный. */
    public Optional<V> get(K key) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Добавляет или обновляет значение; при переполнении вытесняет LRU-запись. */
    public void put(K key, V value) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Текущее число записей. */
    public int size() {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Ключи в порядке от самого старого обращения к самому новому. */
    public List<K> keysInAccessOrder() {
      throw new UnsupportedOperationException("Not implemented yet");
    }
  }

  /**
   * Фабрика кэша. Тесты вызывают её, чтобы выбрать заготовку или эталон; в эталоне возвращается
   * реализация поверх {@code LinkedHashMap}.
   */
  public <K, V> LruCache<K, V> newLruCache(int capacity) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
