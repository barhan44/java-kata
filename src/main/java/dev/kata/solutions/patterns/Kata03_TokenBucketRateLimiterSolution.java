package dev.kata.solutions.patterns;

import dev.kata.patterns.Kata03_TokenBucketRateLimiter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Эталонное решение Kata 03.
 *
 * <h2>Выбор представления состояния</h2>
 *
 * <p>Состояние — это пара «токены + время последнего пополнения». Обе величины должны обновляться
 * атомарно, поэтому используется {@link AtomicReference} на иммутабельном {@link State}. CAS над
 * парой исключает ситуацию, когда поток А обновил токены, а поток Б — время, и в итоге токены
 * «начислились дважды».
 *
 * <p>Альтернатива — упаковать состояние в один {@code long} (токены в фиксированной точке + младшие
 * биты времени) и использовать {@link java.util.concurrent.atomic.AtomicLong}. Это быстрее по
 * памяти и лучше для JMH, но требует ручной битовой арифметики и сужает диапазон времени. Для
 * учебной реализации важнее читаемость, поэтому выбран {@code AtomicReference}.
 *
 * <h2>Почему {@code double} для токенов</h2>
 *
 * <p>При 10 rps и запросах каждые 5 мс за один вызов начисляется 0.05 токена. Если хранить целое
 * число, дробная часть будет обнуляться при каждом обновлении, и фактическая скорость окажется ниже
 * заявленной. {@code double} хранит дробную часть, а решение «пропустить» принимается сравнением с
 * целым {@code permits}.
 *
 * <h2>Почему при отказе нет записи</h2>
 *
 * <p>Ветка «токенов не хватает» не делает CAS. Это важно: иначе каждый отклонённый запрос писал бы
 * в общую cache line, и при высоком RPS отказы создавали бы лишнюю конкуренцию. Отсутствие записи
 * корректно: при следующем успешном CAS пополнение будет пересчитано от старого {@code
 * lastRefillNanos}, то есть дробные токены не потеряются.
 *
 * <h2>Точность и переполнение</h2>
 *
 * <p>Пополнение считается как {@code (now - lastNanos) / 1e9 * rate}. Разность двух значений {@link
 * System#nanoTime()} корректна даже при переполнении {@code long}, пока интервал меньше 2^63 нс.
 * Отрицательная разность (теоретический wrap) трактуется как ноль. Результат всегда ограничивается
 * сверху {@code capacity}, поэтому длительный простой не даёт бесконечного burst.
 */
public final class Kata03_TokenBucketRateLimiterSolution extends Kata03_TokenBucketRateLimiter {

  private static final double NANOS_PER_SECOND = 1_000_000_000.0;

  private final AtomicReference<State> state;

  public Kata03_TokenBucketRateLimiterSolution(long capacity, double refillTokensPerSecond) {
    super(capacity, refillTokensPerSecond);
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be > 0, got " + capacity);
    }
    if (!(refillTokensPerSecond > 0) || Double.isInfinite(refillTokensPerSecond)) {
      throw new IllegalArgumentException(
          "refillTokensPerSecond must be finite and > 0, got " + refillTokensPerSecond);
    }
    this.state = new AtomicReference<>(new State(capacity, System.nanoTime()));
  }

  @Override
  public boolean tryAcquire() {
    return tryAcquire(1);
  }

  @Override
  public boolean tryAcquire(int permits) {
    if (permits <= 0) {
      throw new IllegalArgumentException("permits must be > 0, got " + permits);
    }
    if (permits > capacity) {
      return false;
    }

    while (true) {
      State current = state.get();
      long now = System.nanoTime();
      double tokens = refill(current, now);

      if (tokens < permits) {
        // Отказ не пишет состояние — см. Javadoc класса.
        return false;
      }

      State next = new State(tokens - permits, now);
      if (state.compareAndSet(current, next)) {
        return true;
      }
      // CAS провалился: другой поток успел обновить состояние. Перечитываем и
      // пересчитываем пополнение от его lastNanos — иначе можно начислить дважды.
    }
  }

  @Override
  public double availableTokens() {
    return refill(state.get(), System.nanoTime());
  }

  /** Пополняет ведро по прошедшему времени и ограничивает ёмкостью. */
  private double refill(State current, long now) {
    long elapsedNanos = now - current.lastRefillNanos();
    if (elapsedNanos <= 0) {
      return current.tokens();
    }
    double refilled = current.tokens() + elapsedNanos / NANOS_PER_SECOND * refillTokensPerSecond;
    return Math.min(capacity, refilled);
  }

  /**
   * Иммутабельный снимок состояния.
   *
   * @param tokens текущее (дробное) количество токенов
   * @param lastRefillNanos время, на которое рассчитано {@code tokens}
   */
  private record State(double tokens, long lastRefillNanos) {}
}
