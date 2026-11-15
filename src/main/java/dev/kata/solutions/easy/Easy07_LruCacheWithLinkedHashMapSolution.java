package dev.kata.solutions.easy;

import dev.kata.easy.Easy07_LruCacheWithLinkedHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Эталон Easy 07.
 *
 * <p>{@code LinkedHashMap(capacity, 0.75f, true)} хранит записи в порядке последнего доступа:
 * {@code get} и повторный {@code put} перемещают ключ в конец. Переопределённый {@code
 * removeEldestEntry} возвращает {@code true}, когда размер превысил ёмкость, — вытеснение
 * происходит автоматически внутри {@code put}, без отдельного поиска жертвы.
 *
 * <p>{@code keysInAccessOrder()} копирует ключи в том же порядке: самый старый — первый. Копия
 * неизменяема, поэтому внешняя мутация не влияет на кэш. Нулевые ключи/значения отклоняются: {@code
 * LinkedHashMap} допускает {@code null}, но для кэша это скрытая ошибка вызывающего кода.
 */
public final class Easy07_LruCacheWithLinkedHashMapSolution
    extends Easy07_LruCacheWithLinkedHashMap {

  @Override
  public <K, V> LruCache<K, V> newLruCache(int capacity) {
    return new LinkedHashMapLruCache<>(capacity);
  }

  private static final class LinkedHashMapLruCache<K, V> extends LruCache<K, V> {

    private final LinkedHashMap<K, V> entries;

    private LinkedHashMapLruCache(int capacity) {
      super(capacity);
      this.entries =
          new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
              return size() > capacity;
            }
          };
    }

    @Override
    public Optional<V> get(K key) {
      Objects.requireNonNull(key, "key");
      return Optional.ofNullable(entries.get(key));
    }

    @Override
    public void put(K key, V value) {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(value, "value");
      entries.put(key, value);
    }

    @Override
    public int size() {
      return entries.size();
    }

    @Override
    public List<K> keysInAccessOrder() {
      return List.copyOf(entries.keySet());
    }
  }
}
