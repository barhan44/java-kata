package dev.kata.concurrency;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Kata 08. Hedged Requests: спекулятивный запрос к репликам на виртуальных потоках.
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Сервис читает данные из кластера реплик. В нормальном состоянии достаточно обратиться к одной
 * реплике, но хвост распределения задержек (p99) в 10–50 раз длиннее медианы: одна «задумавшаяся»
 * реплика способна испортить latency всего запроса. Классическое решение из статьи Дина и Баррозу
 * «The Tail at Scale» — <b>hedged request</b>: если первая реплика не ответила за {@code
 * hedgeDelay}, тот же запрос отправляется второй реплике <b>параллельно</b>, не отменяя первый.
 * Побеждает тот ответ, который пришёл раньше; проигравшие запросы отменяются.
 *
 * <p>Виртуальные потоки ({@link java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()})
 * позволяют держать тысячи таких «ожидающих» запросов дёшево: виртуальный поток — это не поток ОС,
 * а объект в куче, который паркуется на I/O без отдельного стека ОС.
 *
 * <h2>Требования</h2>
 *
 * <ol>
 *   <li>Первая реплика запускается немедленно в отдельном виртуальном потоке.
 *   <li>Если за {@code hedgeDelay} successful-ответа нет, запускается вторая реплика. Первая при
 *       этом <b>не отменяется</b>: именно она может ответить следующей.
 *   <li>Если реплик больше двух, допускается последовательное «хеджирование»: следующий запрос
 *       запускается, если с момента предыдущего запуска прошёл {@code hedgeDelay}, а успеха всё ещё
 *       нет.
 *   <li>Первый успешно завершившийся запрос определяет результат. Все остальные запущенные задачи
 *       должны быть прерваны ({@link java.util.concurrent.Future#cancel(boolean)} с {@code
 *       mayInterruptIfRunning = true}).
 *   <li>Если <b>все</b> реплики завершились ошибкой (включая таймауты), выбрасывается {@link
 *       AllReplicasFailedException}, содержащее исходные ошибки в порядке реплик.
 *   <li>Отказ одной реплики не должен ждать {@code hedgeDelay}: следующая реплика запускается
 *       немедленно.
 * </ol>
 *
 * <h2>Сложность и аллокации</h2>
 *
 * <ul>
 *   <li>Время ожидания в худшем случае — {@code hedgeDelay * replicas + longestCallable}, при этом
 *       успешный ответ возвращается сразу по завершении любой задачи.
 *   <li>Память — O(1) на реплику: виртуальный поток, future и одна запись результата в очереди
 *       завершений. Сами {@code Callable} принадлежат вызывающему и не копируются.
 * </ul>
 *
 * <h2>Подводные камни</h2>
 *
 * <ul>
 *   <li><b>Гонка «реплика упала одновременно с хеджем».</b> Таймер и обработчик ошибки могут
 *       попытаться запустить одну и ту же реплику; запуск должен быть защищён CAS.
 *   <li><b>Отмена после победы.</b> {@code Future.cancel(true)} прерывает поток, но задача может
 *       игнорировать прерывание. Контракт: {@code Callable} обязан реагировать на {@link
 *       InterruptedException} (спать/ждать с прерыванием), иначе {@link #execute(List)} не сможет
 *       гарантировать быстрое завершение.
 *   <li><b>Порядок ошибок.</b> Ошибки приходят в порядке завершения, а не в порядке реплик.
 *       Агрегированное исключение должно быть детерминированным — складывайте их по индексу.
 *   <li><b>Утечка потоков.</b> Executor и таймер должны закрываться в {@code finally}, даже если
 *       вызывающий поток сам был прерван.
 *   <li><b>Успех после победы.</b> Две реплики могут завершиться успешно одновременно. Победитель
 *       должен определяться атомарно, иначе результат будет зависеть от планировщика.
 * </ul>
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>{@code replicas == null} или {@code null} среди элементов — {@link NullPointerException}.
 *   <li>Пустой список — {@link IllegalArgumentException}.
 *   <li>{@code hedgeDelay <= 0} — {@link IllegalArgumentException}: нулевая задержка превращает
 *       хеджирование в «запустить все реплики сразу».
 *   <li>Одна реплика: хеджировать нечего, ошибка сразу агрегируется.
 *   <li>Реплика вернула {@code null} — это успешный результат; {@code execute} возвращает {@code
 *       null} (в отличие от кэшей, здесь {@code null} допустим).
 * </ul>
 *
 * <p>Реализацию см. в {@code
 * dev.kata.solutions.concurrency.Kata08_StructuredScatterGatherSolution}. Тесты: {@code
 * Kata08_StructuredScatterGatherTest}.
 */
public class Kata08_StructuredScatterGather {

  /** Задержка перед запуском следующей реплики. */
  protected final Duration hedgeDelay;

  /**
   * @param hedgeDelay время ожидания ответа текущей реплики перед запуском следующей; должно быть
   *     строго больше нуля
   */
  public Kata08_StructuredScatterGather(Duration hedgeDelay) {
    this.hedgeDelay = hedgeDelay;
  }

  /**
   * Выполняет запрос к репликам по стратегии hedged request.
   *
   * @param replicas упорядоченный список реплик; первая — основная, остальные — хеджи
   * @param <T> тип результата
   * @return результат первой успешно ответившей реплики
   * @throws AllReplicasFailedException если все реплики завершились ошибкой
   */
  public <T> T execute(List<? extends Callable<T>> replicas) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Агрегированная ошибка: ни одна реплика не ответила успешно.
   *
   * <p>{@link #failures()} возвращает ошибки в порядке реплик (индекс 0 — первая реплика). Каждая
   * ошибка также добавлена как suppressed к этому исключению, поэтому стандартный стектрейс
   * содержит полную картину.
   */
  public static final class AllReplicasFailedException extends RuntimeException {

    private final List<Throwable> failures;

    /**
     * @param failures ошибки реплик в порядке их индексов; ни один элемент не может быть {@code
     *     null}
     */
    public AllReplicasFailedException(List<? extends Throwable> failures) {
      super("All " + failures.size() + " replica attempts failed");
      this.failures = List.copyOf(failures);
      for (Throwable failure : this.failures) {
        addSuppressed(failure);
      }
    }

    /**
     * @return неизменяемый список ошибок в порядке реплик
     */
    public List<Throwable> failures() {
      return failures;
    }
  }
}
