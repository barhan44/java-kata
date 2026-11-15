package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy09_SequencedHistoryTracker.RecentHistory;
import dev.kata.solutions.easy.Easy09_SequencedHistoryTrackerSolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 09: Sequenced recent history")
class Easy09_SequencedHistoryTrackerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy09_SequencedHistoryTracker newSolution() {
    return USE_SOLUTION
        ? new Easy09_SequencedHistoryTrackerSolution()
        : new Easy09_SequencedHistoryTracker();
  }

  @Test
  @DisplayName("История идёт от самого нового к самому старому")
  void newestFirst() {
    RecentHistory<String> history = newSolution().newHistory(3);
    history.record("a");
    history.record("b");
    history.record("c");

    assertThat(history.getRecent()).containsExactly("c", "b", "a");
  }

  @Test
  @DisplayName("Повторный просмотр поднимает элемент без дубликата")
  void duplicateMovesToFront() {
    RecentHistory<String> history = newSolution().newHistory(3);
    history.record("a");
    history.record("b");
    history.record("c");

    history.record("a");

    assertThat(history.getRecent()).containsExactly("a", "c", "b");
  }

  @Test
  @DisplayName("При переполнении вытесняется самый старый")
  void evictsOldest() {
    RecentHistory<String> history = newSolution().newHistory(3);
    history.record("a");
    history.record("b");
    history.record("c");
    history.record("d");

    assertThat(history.getRecent()).containsExactly("d", "c", "b");
  }

  @Test
  @DisplayName("Лимит 1 и повторные просмотры")
  void maxSizeOne() {
    RecentHistory<String> history = newSolution().newHistory(1);
    history.record("a");
    history.record("b");
    history.record("a");

    assertThat(history.getRecent()).containsExactly("a");
  }

  @Test
  @DisplayName("Пустая история")
  void emptyHistory() {
    RecentHistory<Integer> history = newSolution().newHistory(5);
    assertThat(history.getRecent()).isEmpty();
    assertThatThrownBy(() -> history.getRecent().add(1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Некорректный лимит и null-элемент")
  void validation() {
    assertThatThrownBy(() -> newSolution().newHistory(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().newHistory(-2))
        .isInstanceOf(IllegalArgumentException.class);

    RecentHistory<String> history = newSolution().newHistory(2);
    assertThatThrownBy(() -> history.record(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("getRecent возвращает неизменяемый снимок")
  void immutableSnapshot() {
    RecentHistory<String> history = newSolution().newHistory(2);
    history.record("a");

    var snapshot = history.getRecent();
    history.record("b");

    assertThat(snapshot).containsExactly("a");
    assertThat(history.getRecent()).containsExactly("b", "a");
    assertThatThrownBy(() -> snapshot.add("c")).isInstanceOf(UnsupportedOperationException.class);
  }
}
