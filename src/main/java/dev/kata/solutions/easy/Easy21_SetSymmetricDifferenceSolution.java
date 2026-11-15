package dev.kata.solutions.easy;

import dev.kata.easy.Easy21_SetSymmetricDifference;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Эталон Easy 21.
 *
 * <p>Симметрическая разность = (a \ b) ∪ (b \ a). Оба потока фильтруются по {@code contains}
 * второго множества; {@code LinkedHashSet} убирает теоретические дубли и сохраняет порядок
 * добавления. {@code isSubset} делегируется {@code containsAll} — это уже готовая операция {@code
 * Set}, не требующая ручного перебора.
 *
 * <p>Входные множества не изменяются; возвращаемое множество обёрнуто в unmodifiable.
 */
public final class Easy21_SetSymmetricDifferenceSolution extends Easy21_SetSymmetricDifference {

  @Override
  public <T> Set<T> symmetricDifference(Set<T> a, Set<T> b) {
    Objects.requireNonNull(a, "a");
    Objects.requireNonNull(b, "b");

    Set<T> result = new LinkedHashSet<>();
    a.stream().filter(element -> !b.contains(element)).forEach(result::add);
    b.stream().filter(element -> !a.contains(element)).forEach(result::add);
    return Collections.unmodifiableSet(result);
  }

  @Override
  public <T> boolean isSubset(Set<T> candidate, Set<T> superset) {
    Objects.requireNonNull(candidate, "candidate");
    Objects.requireNonNull(superset, "superset");
    return superset.containsAll(candidate);
  }
}
