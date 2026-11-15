package dev.kata.easy;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Easy 02. Группировка сотрудников по отделам (Collectors).
 *
 * <h2>Задача</h2>
 *
 * <p>Дана модель {@link Employee}. Реализовать два отчёта:
 *
 * <ol>
 *   <li>{@code employeeNamesByDepartment} — отсортированные по алфавиту имена сотрудников по
 *       отделам;
 *   <li>{@code averageSalaryByDepartment} — средняя зарплата по отделу.
 * </ol>
 *
 * <h2>Пример</h2>
 *
 * <pre>
 * employees = [
 *   Employee("Alice", "Engineering", 120),
 *   Employee("Bob",   "Engineering", 80),
 *   Employee("Carol", "Sales", 90)
 * ]
 *
 * employeeNamesByDepartment → {"Engineering": ["Alice","Bob"], "Sales": ["Carol"]}
 * averageSalaryByDepartment → {"Engineering": 100.0, "Sales": 90.0}
 * </pre>
 *
 * <h2>Ограничения</h2>
 *
 * <ul>
 *   <li>0 ≤ {@code employees.size()};
 *   <li>имена и отделы не {@code null}/не blank, зарплата ≥ 0.
 * </ul>
 *
 * <h2>Целевая сложность</h2>
 *
 * <p>Время O(n + u log u), память O(n), где u — число отделов. Эталон использует {@code groupingBy}
 * с {@code mapping}/{@code collectingAndThen} для имён и {@code averagingInt} для зарплат. Карты и
 * списки неизменяемы.
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.easy.Easy02_GroupAndTransformSolution}.
 */
public class Easy02_GroupAndTransform {

  /** Сотрудник. */
  public record Employee(String name, String department, int salary) {
    public Employee {
      requireText(name, "name");
      requireText(department, "department");
      if (salary < 0) {
        throw new IllegalArgumentException("salary must be >= 0, got " + salary);
      }
    }
  }

  /** Возвращает имена по отделам, отсортированные внутри каждого отдела. */
  public Map<String, List<String>> employeeNamesByDepartment(List<Employee> employees) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /** Возвращает среднюю зарплату по каждому отделу. */
  public Map<String, Double> averageSalaryByDepartment(List<Employee> employees) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
