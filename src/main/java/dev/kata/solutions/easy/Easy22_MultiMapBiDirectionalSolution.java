package dev.kata.solutions.easy;

import dev.kata.easy.Easy22_MultiMapBiDirectional;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Эталон Easy 22.
 *
 * <p>Две {@link HashMap}: {@code byKey} и {@code byValue}. {@code put} сначала снимает все старые
 * связи, которые могут конфликтовать: значение, ранее привязанное к ключу, и ключ, ранее
 * привязанный к значению. Только после этого вставляются новые пары. Это гарантирует инвариант
 * 1-к-1 в любой момент после завершения операции.
 *
 * <p>{@code getValue}/{@code getKey} возвращают {@link Optional}, поэтому отсутствие связи — не
 * {@code null}. Нулевые ключи и значения запрещены: иначе {@code containsKey} и {@code get}
 * перестают различать «нет связи» и «связь с null».
 */
public final class Easy22_MultiMapBiDirectionalSolution extends Easy22_MultiMapBiDirectional {

  @Override
  public <K, V> BiDirectionalLookup<K, V> newLookup() {
    return new HashMapLookup<>();
  }

  private static final class HashMapLookup<K, V> extends BiDirectionalLookup<K, V> {

    private final Map<K, V> byKey = new HashMap<>();
    private final Map<V, K> byValue = new HashMap<>();

    @Override
    public void put(K key, V value) {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(value, "value");

      V previousValue = byKey.remove(key);
      if (previousValue != null) {
        byValue.remove(previousValue);
      }
      K previousKey = byValue.remove(value);
      if (previousKey != null) {
        byKey.remove(previousKey);
      }
      byKey.put(key, value);
      byValue.put(value, key);
    }

    @Override
    public Optional<V> getValue(K key) {
      Objects.requireNonNull(key, "key");
      return Optional.ofNullable(byKey.get(key));
    }

    @Override
    public Optional<K> getKey(V value) {
      Objects.requireNonNull(value, "value");
      return Optional.ofNullable(byValue.get(value));
    }

    @Override
    public int size() {
      return byKey.size();
    }
  }
}
