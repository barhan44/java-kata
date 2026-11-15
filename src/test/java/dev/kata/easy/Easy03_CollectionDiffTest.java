package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy03_CollectionDiff.DiffResult;
import dev.kata.easy.Easy03_CollectionDiff.User;
import dev.kata.solutions.easy.Easy03_CollectionDiffSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 03: Collection diff by id")
class Easy03_CollectionDiffTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy03_CollectionDiff newSolution() {
    return USE_SOLUTION ? new Easy03_CollectionDiffSolution() : new Easy03_CollectionDiff();
  }

  @Test
  @DisplayName("Added, removed и unchanged в порядке входных списков")
  void computesAllCategories() {
    List<User> previous = List.of(new User("1", "a@x"), new User("2", "b@x"), new User("3", "c@x"));
    List<User> current = List.of(new User("2", "b2@x"), new User("4", "d@x"), new User("1", "a@x"));

    DiffResult<User> diff = newSolution().diff(previous, current);

    assertThat(diff.added()).extracting(User::id).containsExactly("4");
    assertThat(diff.removed()).extracting(User::id).containsExactly("3");
    assertThat(diff.unchanged()).extracting(User::id).containsExactly("2", "1");
  }

  @Test
  @DisplayName("Unchanged содержит актуальную версию из current")
  void unchangedUsesCurrentVersion() {
    DiffResult<User> diff =
        newSolution().diff(List.of(new User("1", "old@x")), List.of(new User("1", "new@x")));

    assertThat(diff.unchanged()).containsExactly(new User("1", "new@x"));
    assertThat(diff.unchanged().get(0).email()).isEqualTo("new@x");
  }

  @Test
  @DisplayName("Полностью новые и полностью удалённые списки")
  void allAddedAndAllRemoved() {
    List<User> previous = List.of(new User("1", "a@x"));
    List<User> current = List.of(new User("2", "b@x"));

    DiffResult<User> added = newSolution().diff(List.of(), current);
    assertThat(added.added()).containsExactlyElementsOf(current);
    assertThat(added.removed()).isEmpty();
    assertThat(added.unchanged()).isEmpty();

    DiffResult<User> removed = newSolution().diff(previous, List.of());
    assertThat(removed.removed()).containsExactlyElementsOf(previous);
    assertThat(removed.added()).isEmpty();
    assertThat(removed.unchanged()).isEmpty();
  }

  @Test
  @DisplayName("Пустые списки")
  void emptyLists() {
    DiffResult<User> diff = newSolution().diff(List.of(), List.of());
    assertThat(diff.added()).isEmpty();
    assertThat(diff.removed()).isEmpty();
    assertThat(diff.unchanged()).isEmpty();
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    DiffResult<User> diff = newSolution().diff(List.of(), List.of(new User("1", "a@x")));

    assertThatThrownBy(() -> diff.added().add(new User("2", "b@x")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> diff.removed().add(new User("2", "b@x")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> diff.unchanged().add(new User("2", "b@x")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация User и null-аргументов")
  void validation() {
    assertThatThrownBy(() -> new User(null, "a@x")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new User(" ", "a@x")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new User("1", " ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().diff(null, List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().diff(List.of(), null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new DiffResult<User>(null, List.of(), List.of()))
        .isInstanceOf(NullPointerException.class);
  }
}
