package dev.kata.concurrency;

import java.util.Optional;

/**
 * Kata 10. Lock-free стек Трайбера с защитой от ABA-проблемы.
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Стек — базовая структура для work-stealing, пулов объектов и отката транзакций. Блокирующий
 * стек на {@code synchronized} сериализует все операции и плохо масштабируется. Неблокирующий стек
 * Трайбера (R. K. Treiber, 1986) выполняет {@code push}/{@code pop} через CAS над указателем на
 * вершину: потоки не блокируются, а при неудачном CAS повторяют попытку.
 *
 * <h2>ABA-проблема</h2>
 *
 * <p>Пусть вершина стека — узел {@code A}, следующий за ним — {@code B}. Поток T1 читает {@code top
 * = A} и собирается выполнить {@code CAS(A, B)}. Пока T1 приостановлен, T2 делает {@code pop(A)},
 * затем {@code pop(B)}, затем {@code push(A)} — и вершиной снова становится тот же самый объект
 * {@code A}, но его {@code next} теперь указывает не на {@code B}, а на {@code null} (или на другой
 * узел). CAS T1 сравнивает только ссылку, видит {@code A} и успешно заменяет вершину на {@code B},
 * которого в стеке уже нет. Результат: потеря элементов, дубликаты и повреждённая структура.
 *
 * <p>Решение — версионирование: вместе со ссылкой хранить монотонно растущий штамп (номер версии).
 * {@link java.util.concurrent.atomic.AtomicStampedReference} позволяет атомарно сравнить и ссылку,
 * и штамп. Любая модификация стека увеличивает штамп, поэтому «та же ссылка, но другая история»
 * обнаруживается и CAS проваливается.
 *
 * <p>В этой реализации узлы не переиспользуются, поэтому ABA по идентичности ссылки возможна только
 * при повторном {@code push} того же объекта-узла, а не того же значения. Тем не менее контракт
 * структуры данных не должен зависеть от того, кэширует ли вызывающий значения, а штамп —
 * стандартная и самая надёжная защита, которая остаётся корректной даже при будущем добавлении пула
 * узлов.
 *
 * <h2>Требования</h2>
 *
 * <ol>
 *   <li>{@link #push(Object)} добавляет элемент на вершину.
 *   <li>{@link #pop()} удаляет и возвращает верхний элемент; на пустом стеке — {@link
 *       Optional#empty()}.
 *   <li>{@link #peek()} возвращает верхний элемент, не удаляя его.
 *   <li>{@link #isEmpty()} и {@link #size()} дают снимок состояния. {@code size()} может быть
 *       приблизительным под конкуренцией; после завершения всех операций он точен.
 *   <li>{@code push(null)} запрещён: {@link NullPointerException}. {@code Optional} не умеет
 *       хранить {@code null}, и разрешать его — значит терять различие «пусто» и «элемент null».
 *   <li>Операции должны быть lock-free: ни {@code synchronized}, ни {@code ReentrantLock} на
 *       горячем пути.
 * </ol>
 *
 * <h2>Сложность</h2>
 *
 * <ul>
 *   <li>{@code push}, {@code pop}, {@code peek} — O(1) в среднем; при конкуренции возможны
 *       повторные попытки CAS.
 *   <li>Память — O(n), один узел на {@code push}. {@code ThreadLocal<int[]>} для штампа
 *       переиспользует буфер и не аллоцирует на каждую операцию.
 * </ul>
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>{@code pop} на пустом стеке не должен менять штамп и не должен «залипать».
 *   <li>Одновременные {@code pop} не могут вернуть один и тот же элемент.
 *   <li>После {@code push} элемента и немедленного {@code pop} в другом потоке элемент должен быть
 *       виден (happens-before через volatile-семантику CAS).
 *   <li>{@link #size()} после quiescence равен числу оставшихся элементов.
 * </ul>
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.concurrency.Kata10_TreiberStackSolution}. Тесты:
 * {@code Kata10_TreiberStackTest}.
 */
public class Kata10_TreiberStack<T> {

  /**
   * Добавляет элемент на вершину стека.
   *
   * @param item элемент; не {@code null}
   */
  public void push(T item) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Удаляет и возвращает верхний элемент.
   *
   * @return верхний элемент или {@link Optional#empty()}, если стек пуст
   */
  public Optional<T> pop() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Возвращает верхний элемент, не удаляя его.
   *
   * @return верхний элемент или {@link Optional#empty()}, если стек пуст
   */
  public Optional<T> peek() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * @return {@code true}, если стек пуст на момент вызова
   */
  public boolean isEmpty() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Приблизительное количество элементов. Под конкуренцией значение может быть неточным; после
   * завершения всех операций оно корректно.
   *
   * @return число элементов
   */
  public int size() {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
