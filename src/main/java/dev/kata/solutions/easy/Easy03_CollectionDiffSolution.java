package dev.kata.solutions.easy;

import dev.kata.easy.Easy03_CollectionDiff;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Эталон Easy 03.
 *
 * <p>Сначала строим множества id обеих сторон — это даёт O(1) проверку принадлежности. Затем три
 * независимых фильтра: текущий без предыдущих, предыдущий без текущих и текущий с общими id.
 * Исходные списки не сортируются и не изменяются, поэтому порядок сохраняется.
 *
 * <p>Для {@code unchanged} берём элемент из {@code current}: если у пользователя изменился email,
 * актуальной считается новая версия. {@code toList()} + копирующий конструктор {@link DiffResult}
 * делают результат иммутабельным.
 */
public final class Easy03_CollectionDiffSolution extends Easy03_CollectionDiff {

  @Override
  public DiffResult<User> diff(List<User> previous, List<User> current) {
    Objects.requireNonNull(previous, "previous");
    Objects.requireNonNull(current, "current");

    Set<String> previousIds = previous.stream().map(User::id).collect(Collectors.toSet());
    Set<String> currentIds = current.stream().map(User::id).collect(Collectors.toSet());

    List<User> added = current.stream().filter(user -> !previousIds.contains(user.id())).toList();
    List<User> removed = previous.stream().filter(user -> !currentIds.contains(user.id())).toList();
    List<User> unchanged =
        current.stream().filter(user -> previousIds.contains(user.id())).toList();

    return new DiffResult<>(added, removed, unchanged);
  }
}
