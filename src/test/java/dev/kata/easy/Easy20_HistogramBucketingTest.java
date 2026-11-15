package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.easy.Easy20_HistogramBucketingSolution;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 20: Latency histogram bucketing")
class Easy20_HistogramBucketingTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy20_HistogramBucketing newSolution() {
    return USE_SOLUTION ? new Easy20_HistogramBucketingSolution() : new Easy20_HistogramBucketing();
  }

  @Test
  @DisplayName("Распределяет значения по корзинам")
  void bucketsValues() {
    Map<String, Long> result =
        newSolution().buildLatencyHistogram(List.of(0L, 5L, 10L, 11L, 60L, 201L));

    assertThat(result)
        .containsEntry("0-10ms", 3L)
        .containsEntry("11-50ms", 1L)
        .containsEntry("51-200ms", 1L)
        .containsEntry("200ms+", 1L);
  }

  @Test
  @DisplayName("Границы 10/50/200 включительны, 201 уже в последней корзине")
  void boundaryValues() {
    Map<String, Long> result =
        newSolution().buildLatencyHistogram(List.of(10L, 11L, 50L, 51L, 200L, 201L));

    assertThat(result)
        .containsEntry("0-10ms", 1L)
        .containsEntry("11-50ms", 2L)
        .containsEntry("51-200ms", 2L)
        .containsEntry("200ms+", 1L);
  }

  @Test
  @DisplayName("Пустой список даёт все четыре ключа с нулями")
  void emptyHistogramHasAllKeys() {
    Map<String, Long> result = newSolution().buildLatencyHistogram(List.of());

    assertThat(result.keySet()).containsExactly("0-10ms", "11-50ms", "51-200ms", "200ms+");
    assertThat(result.values()).containsOnly(0L);
  }

  @Test
  @DisplayName("Отрицательная задержка и null-элемент отклоняются")
  void invalidValues() {
    assertThatThrownBy(() -> newSolution().buildLatencyHistogram(List.of(-1L)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().buildLatencyHistogram(Arrays.asList(1L, null)))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("null-список отклоняется")
  void nullList() {
    assertThatThrownBy(() -> newSolution().buildLatencyHistogram(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    Map<String, Long> result = newSolution().buildLatencyHistogram(List.of(1L));
    assertThatThrownBy(() -> result.put("x", 1L)).isInstanceOf(UnsupportedOperationException.class);
  }
}
