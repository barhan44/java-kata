package dev.kata.solutions.easy;

import dev.kata.easy.Easy15_StreamZipAndIndex;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Эталон Easy 15.
 *
 * <p>Длина результата — {@code min(first.size(), second.size())}: лишние элементы длинного списка
 * игнорируются. {@code IntStream.range} даёт индексы без ручного счётчика, {@code mapToObj}
 * вызывает переданную функцию. Оба списка только читаются, результат {@code toList()} неизменяем.
 */
public final class Easy15_StreamZipAndIndexSolution extends Easy15_StreamZipAndIndex {

  @Override
  public <A, B, R> List<R> zipWithIndex(
      List<A> first, List<B> second, TriFunction<Integer, A, B, R> combiner) {
    Objects.requireNonNull(first, "first");
    Objects.requireNonNull(second, "second");
    Objects.requireNonNull(combiner, "combiner");

    int size = Math.min(first.size(), second.size());
    return IntStream.range(0, size)
        .mapToObj(index -> combiner.apply(index, first.get(index), second.get(index)))
        .toList();
  }
}
