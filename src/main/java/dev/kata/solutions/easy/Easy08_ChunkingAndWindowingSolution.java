package dev.kata.solutions.easy;

import dev.kata.easy.Easy08_ChunkingAndWindowing;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Эталон Easy 08.
 *
 * <p>{@code chunk} берёт только индексы, кратные размеру куска, и для каждого копирует {@code
 * subList}(с защитой от выхода за конец). Копирование обязательно: {@code subList} — это view,
 * который держит ссылку на исходный список и меняется вместе с ним. {@code List.copyOf} делает
 * вложенные списки неизменяемыми.
 *
 * <p>{@code zipWithNext} — {@code IntStream.range(0, size - 1)}; для пустого и одноэлементного
 * списка диапазон пуст. Обе операции линейны и не используют ручную мутацию индексов.
 */
public final class Easy08_ChunkingAndWindowingSolution extends Easy08_ChunkingAndWindowing {

  @Override
  public <T> List<List<T>> chunk(List<T> source, int chunkSize) {
    Objects.requireNonNull(source, "source");
    if (chunkSize <= 0) {
      throw new IllegalArgumentException("chunkSize must be > 0, got " + chunkSize);
    }

    return IntStream.range(0, source.size())
        .filter(index -> index % chunkSize == 0)
        .mapToObj(
            index -> List.copyOf(source.subList(index, Math.min(index + chunkSize, source.size()))))
        .toList();
  }

  @Override
  public <T> List<Pair<T, T>> zipWithNext(List<T> source) {
    Objects.requireNonNull(source, "source");

    return IntStream.range(0, source.size() - 1)
        .mapToObj(index -> new Pair<>(source.get(index), source.get(index + 1)))
        .toList();
  }
}
