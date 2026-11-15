package dev.kata.patterns;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.LongSupplier;

/**
 * Kata 11. Circuit Breaker со скользящим окном и детерминированными часами.
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Когда удалённый сервис деградирует, продолжать слать ему запросы — значит усугублять проблему:
 * каждый вызов висит до таймаута, занимает потоки и исчерпывает пул соединений. Circuit Breaker
 * (предохранитель) быстро отклоняет вызовы, пока сервис «остывает», и периодически пропускает
 * пробные запросы, чтобы понять, восстановился ли он.
 *
 * <h2>Состояния</h2>
 *
 * <pre>
 *                 failure rate > threshold
 *      CLOSED  --------------------------->  OPEN
 *        ^                                     |
 *        |                                     | openDuration истёк
 *        |  все пробные запросы успешны        v
 *        +-------------------------------- HALF_OPEN
 *                     любой пробный запрос упал --> OPEN
 * </pre>
 *
 * <h2>Требования</h2>
 *
 * <ol>
 *   <li><b>CLOSED.</b> Вызовы проходят. Результаты последних {@code slidingWindowSize} вызовов
 *       хранятся в кольцевом буфере (успех/ошибка). Как только набрано не меньше {@code
 *       minimumCalls} и доля ошибок строго больше {@code failureRateThreshold}, автомат переходит в
 *       {@code OPEN}.
 *   <li><b>OPEN.</b> Все вызовы немедленно отклоняются {@link CircuitBreakerOpenException}, целевая
 *       функция не вызывается. Через {@code openDuration} автомат переходит в {@code HALF_OPEN}.
 *       Переход ленивый: он происходит при следующем {@code execute}/{@code currentState}, а не по
 *       таймеру.
 *   <li><b>HALF_OPEN.</b> Пропускается не более {@code permittedCallsInHalfOpen} пробных запросов
 *       одновременно; остальные отклоняются. Если все разрешённые пробы успешны — переход в {@code
 *       CLOSED} (окно очищается). Любая неуспешная проба немедленно возвращает {@code OPEN} и
 *       сбрасывает отсчёт {@code openDuration}.
 *   <li><b>Время.</b> Источник времени — {@link LongSupplier} в наносекундах (по умолчанию {@link
 *       System#nanoTime()}). Это позволяет тестировать переходы без {@code Thread.sleep} и
 *       гарантирует монотонность. {@link System#currentTimeMillis()} здесь опасен: перевод часов
 *       может «заморозить» или «мгновенно открыть» предохранитель.
 *   <li>Пользовательская функция <b>никогда</b> не выполняется под внутренней блокировкой:
 *       медленный вызов не должен останавливать остальные потоки, проверяющие состояние.
 * </ol>
 *
 * <h2>Конкурентность</h2>
 *
 * <ul>
 *   <li>Проверка допуска и обновление состояния — под одной блокировкой; это делает переходы
 *       атомарными. В состоянии {@code HALF_OPEN} ровно {@code permittedCallsInHalfOpen} потоков
 *       получают разрешение, остальные — отказ.
 *   <li>Вызовы, начатые в {@code CLOSED}, могут завершиться уже после перехода в {@code OPEN}. Их
 *       результаты не должны влиять на новое состояние: при допуске запоминается «эпоха»
 *       (поколение), и результаты из старой эпохи игнорируются.
 *   <li>То же для проб: после неудачи одной пробы остальные пробные вызовы ещё выполняются, но их
 *       результаты уже не могут перевести автомат в {@code CLOSED}.
 * </ul>
 *
 * <h2>Сложность</h2>
 *
 * <ul>
 *   <li>{@code execute} — O(1): проверка состояния, один вызов функции, запись в кольцо O(1).
 *   <li>Память — O({@code slidingWindowSize}) на кольцо, независимо от числа вызовов.
 * </ul>
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>{@code slidingWindowSize <= 0}, {@code minimumCalls <= 0}, {@code minimumCalls >
 *       slidingWindowSize}, {@code failureRateThreshold} вне {@code (0, 1]}, {@code openDuration <=
 *       0}, {@code permittedCallsInHalfOpen <= 0}, {@code null} часы — {@link
 *       IllegalArgumentException} / {@link NullPointerException}.
 *   <li>Ошибка целевой функции пробрасывается вызывающему без изменений и учитывается как failure.
 *       {@link Error} также считается ошибкой.
 *   <li>{@code openDuration} истёк ровно в момент вызова — считается истёкшим ({@code now -
 *       openedAt >= duration}).
 *   <li>Часы «прыгнули назад» — переход не происходит, пока разность не станет неотрицательной.
 *   <li>{@link #rejectedCalls()} — счётчик отклонённых вызовов для диагностики/метрик.
 * </ul>
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.patterns.Kata11_CircuitBreakerSolution}. Тесты:
 * {@code Kata11_CircuitBreakerTest}.
 */
public class Kata11_CircuitBreaker {

  /** Состояние автомата. */
  public enum State {
    CLOSED,
    OPEN,
    HALF_OPEN
  }

  protected final int slidingWindowSize;
  protected final int minimumCalls;
  protected final double failureRateThreshold;
  protected final Duration openDuration;
  protected final int permittedCallsInHalfOpen;
  protected final LongSupplier nanoTimeSource;

  /**
   * Создаёт предохранитель с системными монотонными часами.
   *
   * @param slidingWindowSize размер кольцевого буфера последних результатов
   * @param minimumCalls минимальное число вызовов в окне для оценки доли ошибок
   * @param failureRateThreshold порог доли ошибок в интервале {@code (0, 1]}
   * @param openDuration сколько времени оставаться в {@code OPEN}
   * @param permittedCallsInHalfOpen сколько пробных вызовов разрешать в {@code HALF_OPEN}
   */
  public Kata11_CircuitBreaker(
      int slidingWindowSize,
      int minimumCalls,
      double failureRateThreshold,
      Duration openDuration,
      int permittedCallsInHalfOpen) {
    this(
        slidingWindowSize,
        minimumCalls,
        failureRateThreshold,
        openDuration,
        permittedCallsInHalfOpen,
        System::nanoTime);
  }

  /**
   * Создаёт предохранитель с явным источником времени.
   *
   * @param nanoTimeSource монотонное время в наносекундах
   */
  public Kata11_CircuitBreaker(
      int slidingWindowSize,
      int minimumCalls,
      double failureRateThreshold,
      Duration openDuration,
      int permittedCallsInHalfOpen,
      LongSupplier nanoTimeSource) {
    this.slidingWindowSize = slidingWindowSize;
    this.minimumCalls = minimumCalls;
    this.failureRateThreshold = failureRateThreshold;
    this.openDuration = openDuration;
    this.permittedCallsInHalfOpen = permittedCallsInHalfOpen;
    this.nanoTimeSource = nanoTimeSource;
  }

  /**
   * Выполняет вызов через предохранитель.
   *
   * @param call целевая функция
   * @param <T> тип результата
   * @return результат функции
   * @throws CircuitBreakerOpenException если вызов отклонён без выполнения
   * @throws Exception исключение целевой функции (пробрасывается как есть)
   */
  public <T> T execute(Callable<T> call) throws Exception {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Текущее состояние. Если {@code openDuration} уже истёк, лениво переводит {@code OPEN} в {@code
   * HALF_OPEN}.
   *
   * @return состояние автомата
   */
  public State currentState() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * @return число вызовов, отклонённых в состоянии {@code OPEN}/{@code HALF_OPEN}
   */
  public long rejectedCalls() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /** Исключение «предохранитель открыт»: вызов не дошёл до целевой функции. */
  public static final class CircuitBreakerOpenException extends RuntimeException {

    private final State state;

    /**
     * @param state состояние, в котором произошёл отказ ({@code OPEN} или {@code HALF_OPEN})
     */
    public CircuitBreakerOpenException(State state) {
      super("Circuit breaker is " + state + ": call rejected");
      this.state = state;
    }

    /**
     * @return состояние в момент отказа
     */
    public State state() {
      return state;
    }
  }
}
