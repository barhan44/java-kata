package dev.kata.solutions.easy;

import dev.kata.easy.Easy25_SlidingWindowMaximumDeque;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Эталон Easy 25.
 *
 * <p>Монотонная очередь хранит индексы, а не значения; значения извлекаются из {@code nums}. На
 * каждом шаге:
 *
 * <ol>
 *   <li>из головы удаляются индексы, которые вышли за границу {@code i - k};
 *   <li>из хвоста удаляются индексы со значением ≤ текущего — они уже не могут стать максимумом;
 *   <li>текущий индекс добавляется в хвост;
 *   <li>когда окно набрано, голова очереди даёт максимум.
 * </ol>
 *
 * <p>Каждый индекс попадает в очередь один раз и удаляется один раз — суммарно O(n). Результат —
 * новый массив, вход не изменяется.
 */
public final class Easy25_SlidingWindowMaximumDequeSolution
    extends Easy25_SlidingWindowMaximumDeque {

  @Override
  public int[] maxSlidingWindow(int[] nums, int k) {
    Objects.requireNonNull(nums, "nums");
    if (nums.length == 0 || k <= 0 || k > nums.length) {
      throw new IllegalArgumentException(
          "Require 1 <= k <= nums.length, got k=" + k + ", length=" + nums.length);
    }

    int[] result = new int[nums.length - k + 1];
    Deque<Integer> deque = new ArrayDeque<>();

    for (int index = 0; index < nums.length; index++) {
      while (!deque.isEmpty() && deque.peekFirst() <= index - k) {
        deque.removeFirst();
      }
      while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[index]) {
        deque.removeLast();
      }
      deque.addLast(index);

      if (index >= k - 1) {
        result[index - k + 1] = nums[deque.peekFirst()];
      }
    }
    return result;
  }
}
