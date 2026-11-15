package dev.kata.distributed;

import java.util.Set;

/**
 * Kata 24. OR-Set: state-based CRDT «наблюдаемое удаление» (Add-Wins).
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Реплики edge-кэша/корзины работают без координации: пользователь добавляет товар на одной
 * реплике, удаляет на другой, а сеть может доставить операции в любом порядке. Нужна структура,
 * которая после слияния состояния всех реплик приходит к одному результату (Strong Eventual
 * Consistency). Наивный 2P-Set запрещает повторное добавление удалённого элемента; OR-Set решает
 * это, присваивая каждому добавлению уникальный тег.
 *
 * <h2>Модель</h2>
 *
 * <ul>
 *   <li>Состояние реплики: {@code adds: Map<T, Set<Tag>>} и {@code removed: Set<Tag>}.
 *   <li>{@code add(x)} создаёт новый тег {@code (replicaId, sequence)} и добавляет его в {@code
 *       adds[x]}. Один и тот же элемент может иметь много тегов.
 *   <li>{@code remove(x)} перемещает <b>все наблюдаемые</b> теги {@code x} в {@code removed}. Теги,
 *       добавленные конкурентно на другой реплике и ещё не доставленные сюда, не затрагиваются.
 *   <li>{@code contains(x)} — есть ли у {@code x} хотя бы один тег, не попавший в {@code removed}.
 *   <li>{@code merge(other)} — объединение {@code adds} и {@code removed}. Это объединение
 *       множеств, поэтому операция коммутативна, ассоциативна и идемпотентна.
 * </ul>
 *
 * <h2>Add-Wins</h2>
 *
 * <pre>
 *   Реплика A: add(x) -> tag A1; remove(x) наблюдает A1
 *   Реплика B: add(x) -> tag B1 (конкурентно)
 *
 *   merge: adds = {A1, B1}, removed = {A1}
 *   B1 не удалён -> элемент присутствует. Побеждает добавление.
 * </pre>
 *
 * <p>Если бы обе реплики наблюдали один и тот же тег и обе удалили его, элемент исчез бы. Это
 * классическая семантика OR-Set (Add-Wins, observed-remove).
 *
 * <h2>Требования</h2>
 *
 * <ol>
 *   <li>Уникальность {@code replicaId}: два узла не должны использовать одинаковый id, иначе теги
 *       могут совпасть. Внутри реплики sequence монотонно растёт.
 *   <li>{@link #merge(Kata24_OrSetCrdt)} принимает снимок другой реплики. Слияние не должно
 *       блокировать обе реплики одновременно (потенциальный deadlock при взаимном merge);
 *       реализация снимает копию под замком {@code other}, затем объединяет под своим замком.
 *   <li>{@link #elements()} возвращает неизменяемую копию; изменение сета не влияет на CRDT.
 *   <li>Элементы могут быть {@code null}; состояние использует {@code HashMap}/{@code HashSet},
 *       которые это допускают.
 * </ol>
 *
 * <h2>Сложность</h2>
 *
 * <ul>
 *   <li>{@code add}/{@code remove}/{@code contains} — O(1) в среднем (remove — O(числа тегов
 *       элемента)).
 *   <li>{@code elements} — O(суммарного числа тегов).
 *   <li>{@code merge} — O(размера состояния other) на снимок + O(размера состояния this) на
 *       объединение.
 * </ul>
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>{@code null} replicaId или пустой id — {@link NullPointerException} / {@link
 *       IllegalArgumentException}.
 *   <li>{@code remove} отсутствующего элемента — no-op.
 *   <li>Повторное добавление после удаления создаёт новый тег и снова делает элемент видимым.
 *   <li>Слияние с самим собой и повторное слияние идемпотентны.
 * </ul>
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.distributed.Kata24_OrSetCrdtSolution}. Тесты:
 * {@code Kata24_OrSetCrdtTest}.
 */
public class Kata24_OrSetCrdt<T> {

  protected final String replicaId;

  /**
   * @param replicaId уникальный идентификатор реплики
   */
  public Kata24_OrSetCrdt(String replicaId) {
    this.replicaId = replicaId;
  }

  /**
   * Добавляет элемент с уникальным тегом.
   *
   * @param element элемент; может быть {@code null}
   */
  public void add(T element) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Удаляет все наблюдаемые теги элемента.
   *
   * @param element элемент; может быть {@code null}
   */
  public void remove(T element) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * @param element элемент; может быть {@code null}
   * @return {@code true}, если у элемента есть хотя бы один не удалённый тег
   */
  public boolean contains(T element) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * @return неизменяемая копия видимых элементов
   */
  public Set<T> elements() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Сливает состояние другой реплики.
   *
   * @param other другая реплика того же типа
   */
  public void merge(Kata24_OrSetCrdt<T> other) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
