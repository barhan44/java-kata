package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy02_GroupAndTransform.Employee;
import dev.kata.solutions.easy.Easy02_GroupAndTransformSolution;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 02: Group and transform employees")
class Easy02_GroupAndTransformTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy02_GroupAndTransform newSolution() {
    return USE_SOLUTION ? new Easy02_GroupAndTransformSolution() : new Easy02_GroupAndTransform();
  }

  @Test
  @DisplayName("Имена группируются и сортируются внутри отдела")
  void namesByDepartment() {
    List<Employee> employees =
        List.of(
            new Employee("Zoe", "Engineering", 100),
            new Employee("Alice", "Engineering", 120),
            new Employee("Bob", "Sales", 90),
            new Employee("Anna", "Sales", 80));

    Map<String, List<String>> result = newSolution().employeeNamesByDepartment(employees);

    assertThat(result.keySet()).containsExactly("Engineering", "Sales");
    assertThat(result.get("Engineering")).containsExactly("Alice", "Zoe");
    assertThat(result.get("Sales")).containsExactly("Anna", "Bob");
  }

  @Test
  @DisplayName("Средняя зарплата считается по каждому отделу")
  void averageSalaryByDepartment() {
    List<Employee> employees =
        List.of(
            new Employee("Alice", "Engineering", 100),
            new Employee("Bob", "Engineering", 200),
            new Employee("Carol", "Sales", 90));

    Map<String, Double> result = newSolution().averageSalaryByDepartment(employees);

    assertThat(result).containsEntry("Engineering", 150.0).containsEntry("Sales", 90.0);
  }

  @Test
  @DisplayName("Пустой список даёт пустые карты")
  void emptyInput() {
    assertThat(newSolution().employeeNamesByDepartment(List.of())).isEmpty();
    assertThat(newSolution().averageSalaryByDepartment(List.of())).isEmpty();
  }

  @Test
  @DisplayName("Дубликаты имён сохраняются")
  void duplicateNames() {
    List<Employee> employees =
        List.of(new Employee("Alex", "QA", 10), new Employee("Alex", "QA", 20));

    assertThat(newSolution().employeeNamesByDepartment(employees).get("QA"))
        .containsExactly("Alex", "Alex");
    assertThat(newSolution().averageSalaryByDepartment(employees).get("QA")).isEqualTo(15.0);
  }

  @Test
  @DisplayName("Карты и списки имён неизменяемы")
  void immutableResults() {
    List<Employee> employees = List.of(new Employee("Alice", "Engineering", 100));
    Map<String, List<String>> names = newSolution().employeeNamesByDepartment(employees);
    Map<String, Double> averages = newSolution().averageSalaryByDepartment(employees);

    assertThatThrownBy(() -> names.put("X", List.of()))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> names.get("Engineering").add("Bob"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> averages.put("X", 1.0))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация Employee и null-списка")
  void validation() {
    assertThatThrownBy(() -> new Employee(null, "Eng", 1)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Employee(" ", "Eng", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Employee("A", " ", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Employee("A", "Eng", -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().employeeNamesByDepartment(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newSolution().averageSalaryByDepartment(null))
        .isInstanceOf(NullPointerException.class);
  }
}
