package dev.kata.solutions.easy;

import dev.kata.easy.Easy02_GroupAndTransform;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Эталон Easy 02.
 *
 * <p>Оба отчёта — один {@code groupingBy}. Для имён downstream {@code
 * collectingAndThen(mapping(...), list -> list.stream().sorted().toList())} сортирует имена после
 * группировки. Для зарплат {@code averagingInt} сам считает сумму и количество, без деления вручную
 * и без риска деления на ноль — пустой отдел просто не создаётся.
 *
 * <p>{@code TreeMap::new} фиксирует алфавитный порядок отделов. {@code toList()} и {@code
 * Collections.unmodifiableMap} не дают изменить отчёт снаружи.
 */
public final class Easy02_GroupAndTransformSolution extends Easy02_GroupAndTransform {

  @Override
  public Map<String, List<String>> employeeNamesByDepartment(List<Employee> employees) {
    Objects.requireNonNull(employees, "employees");

    Map<String, List<String>> byDepartment =
        employees.stream()
            .collect(
                Collectors.groupingBy(
                    Employee::department,
                    TreeMap::new,
                    Collectors.collectingAndThen(
                        Collectors.mapping(Employee::name, Collectors.toList()),
                        names -> names.stream().sorted().toList())));
    return Collections.unmodifiableMap(byDepartment);
  }

  @Override
  public Map<String, Double> averageSalaryByDepartment(List<Employee> employees) {
    Objects.requireNonNull(employees, "employees");

    Map<String, Double> averages =
        employees.stream()
            .collect(
                Collectors.groupingBy(
                    Employee::department, TreeMap::new, Collectors.averagingInt(Employee::salary)));
    return Collections.unmodifiableMap(averages);
  }
}
