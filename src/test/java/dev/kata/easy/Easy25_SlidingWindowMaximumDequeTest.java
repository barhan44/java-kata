package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy25_SlidingWindowMaximumDequeSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 25: Sliding window maximum")
class Easy25_SlidingWindowMaximumDequeTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy25_SlidingWindowMaximumDeque newSolution() {
    return USE_SOLUTION
        ? new Easy25_SlidingWindowMaximumDequeSolution()
        : new Easy25_SlidingWindowMaximumDeque();
  }

  @Test
  @DisplayName("Классический пример")
  void classicExample() {
    assertThat(newSolution().maxSlidingWindow(new int[] {1, 3, -1, -3, 5, 3, 6, 7}, 3))
        .containsExactly(3, 3, 5, 5, 6, 7);
  }

  @Test
  @DisplayName("k = 1 возвращает сам массив, k = n — глобальный максимум")
  void boundaryK() {
    assertThat(newSolution().maxSlidingWindow(new int[] {1, -2, 3}, 1)).containsExactly(1, -2, 3);
    assertThat(newSolution().maxSlidingWindow(new int[] {1, -2, 3}, 3)).containsExactly(3);
  }

  @Test
  @DisplayName("Отрицательные значения и дубликаты")
  void negativesAndDuplicates() {
    assertThat(newSolution().maxSlidingWindow(new int[] {-7, -8, -3, -3, -1}, 2))
        .containsExactly(-7, -3, -3, -1);
    assertThat(newSolution().maxSlidingWindow(new int[] {2, 2, 2}, 2)).containsExactly(2, 2);
  }

  @Test
  @DisplayName("Один элемент")
  void singleElement() {
    assertThat(newSolution().maxSlidingWindow(new int[] {42}, 1)).containsExactly(42);
  }

  @Test
  @DisplayName("Некорректный k и null-массив отклоняются")
  void validation() {
    assertThatThrownBy(() -> newSolution().maxSlidingWindow(new int[] {1, 2}, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().maxSlidingWindow(new int[] {1, 2}, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().maxSlidingWindow(new int[] {1, 2}, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().maxSlidingWindow(new int[] {}, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().maxSlidingWindow(null, 1))
        .isInstanceOf(NullPointerException.class);
  }
}
