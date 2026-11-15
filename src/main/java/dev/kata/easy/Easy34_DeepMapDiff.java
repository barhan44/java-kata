package dev.kata.easy;

import java.util.Map;

/**
 * Easy 34. Diff вложенных мап по листьям.
 *
 * <h2>Задача</h2>
 *
 * <p>Сравнить два вложенных дерева значений и вернуть различия листьев в точечной нотации.
 * Отсутствующая сторона обозначается {@code "<missing>"}; формат значения: {@code "<path>: <left>
 * -> <right>"}. Результат отсортирован по пути.
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * left  = {"server": {"port": 8080}, "feature": {}}
 * right = {"server": {"port": 9090}, "feature": {"flag": true}}
 *
 * diffNestedMaps → {
 *   "feature.flag": "<missing> -> true",
 *   "server.port":  "8080 -> 9090"
 * }
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ размеры мап; ключи — строки, значения — листья или вложенные {@code Map};
 *   <li>вложенные мапы рекурсивны; {@code null} допустим как листовое значение.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(K), память O(K), где K — суммарное число листьев. Эталон рекурсивно «сплющивает» обе
 * мапы в {@code path → value}, затем сравнивает объединение ключей. Если с одной стороны лист, а с
 * другой — вложенная мапа, различия появятся для обоих путей. Результат неизменяем.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy34_DeepMapDiffSolution}.
 */
public class Easy34_DeepMapDiff {

  /** Возвращает различия листьев в формате {@code path: left -> right}. */
  public Map<String, String> diffNestedMaps(Map<String, Object> left, Map<String, Object> right) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
