package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy12_FlatMapDistinctSorted.Course;
import dev.kata.solutions.easy.Easy12_FlatMapDistinctSortedSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 12: FlatMap, distinct and sorted prerequisites")
class Easy12_FlatMapDistinctSortedTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy12_FlatMapDistinctSorted newSolution() {
    return USE_SOLUTION
        ? new Easy12_FlatMapDistinctSortedSolution()
        : new Easy12_FlatMapDistinctSorted();
  }

  @Test
  @DisplayName("Нормализует, исключает существующие курсы и сортирует")
  void missingPrerequisites() {
    List<Course> courses =
        List.of(
            new Course("Java", List.of(" OOP ", "Algorithms")),
            new Course("OOP", List.of("Programming Basics")),
            new Course("Algorithms", List.of()));

    assertThat(newSolution().missingPrerequisites(courses)).containsExactly("programming basics");
  }

  @Test
  @DisplayName("При равной длине — алфавитный порядок")
  void tieBreakByLengthThenAlphabet() {
    List<Course> courses = List.of(new Course("Course", List.of("zeta", "beta", "alpha", "gamma")));

    assertThat(newSolution().missingPrerequisites(courses))
        .containsExactly("beta", "zeta", "alpha", "gamma");
  }

  @Test
  @DisplayName("Дубликаты и blank-пререквизиты удаляются")
  void duplicatesAndBlanks() {
    List<Course> courses =
        List.of(
            new Course("A", List.of("Java", " java ", "", "   ")),
            new Course("B", List.of("JAVA", "spring")));

    assertThat(newSolution().missingPrerequisites(courses)).containsExactly("java", "spring");
  }

  @Test
  @DisplayName("Пустой список и курсы без пререквизитов")
  void emptyCases() {
    assertThat(newSolution().missingPrerequisites(List.of())).isEmpty();
    assertThat(newSolution().missingPrerequisites(List.of(new Course("A", List.of())))).isEmpty();
  }

  @Test
  @DisplayName("Результат неизменяем")
  void immutableResult() {
    List<String> result =
        newSolution().missingPrerequisites(List.of(new Course("A", List.of("b"))));
    assertThatThrownBy(() -> result.add("c")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация Course и null-списка")
  void validation() {
    assertThatThrownBy(() -> new Course(null, List.of())).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Course(" ", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Course("A", null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().missingPrerequisites(null))
        .isInstanceOf(NullPointerException.class);
  }
}
