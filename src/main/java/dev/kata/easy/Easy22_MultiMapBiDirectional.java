package dev.kata.easy;

import java.util.Optional;

/**
 * Easy 22. Двунаправленный lookup 1-к-1.
 *
 * <h2>Задача</h2>
 *
 * <p>Реализовать {@link BiDirectionalLookup}: связь ключа и значения, где каждый ключ имеет ровно
 * одно значение, и каждое значение — ровно один ключ. {@code put} должен корректно разрывать старые
 * конфликтующие связи: если ключ уже был связан с другим значением, старая обратная связь
 * удаляется; если значение уже было связано с другим ключом, удаляется старая прямая связь.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * put("a", 1); put("b", 2)
 * put("a", 2)
 * getValue("a") → Optional[2]
 * getKey(1)     → Optional.empty
 * getValue("b") → Optional.empty
 * size()        → 1
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>ключи и значения не {@code null};
 *   <li>размер не превышает число живых пар.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Все операции O(1) в среднем: две {@link java.util.HashMap} хранят прямую и обратную связь. При
 * {@code put} удаления из обеих карт выполняются до вставки, поэтому промежуточное состояние не
 * наблюдается.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy22_MultiMapBiDirectionalSolution}.
 */
public class Easy22_MultiMapBiDirectional {

  /** Двунаправленная карта. */
  public static class BiDirectionalLookup<K, V> {

    protected BiDirectionalLookup() {}

    /** Связывает ключ и значение, разрывая старые конфликты. */
    public void put(K key, V value) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Значение по ключу. */
    public Optional<V> getValue(K key) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Ключ по значению. */
    public Optional<K> getKey(V value) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Число живых пар. */
    public int size() {
      throw new UnsupportedOperationException("Not implemented yet");
    }
  }

  /** Фабрика lookup: тесты выбирают заготовку или эталон. */
  public <K, V> BiDirectionalLookup<K, V> newLookup() {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
