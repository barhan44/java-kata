package dev.kata.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.easy.Easy08_ChunkingAndWindowing.Pair;
import dev.kata.solutions.easy.Easy08_ChunkingAndWindowingSolution;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Easy 08: Chunking and windowing")
class Easy08_ChunkingAndWindowingTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Easy08_ChunkingAndWindowing newSolution() {
    return USE_SOLUTION
        ? new Easy08_ChunkingAndWindowingSolution()
        : new Easy08_ChunkingAndWindowing();
  }

  @Test
  @DisplayName("Chunk с неполным последним куском")
  void chunkWithRemainder() {
    assertThat(newSolution().chunk(List.of(1, 2, 3, 4, 5), 2))
        .containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
  }

  @Test
  @DisplayName("Chunk с точным делением и размером больше списка")
  void chunkExactAndOversized() {
    assertThat(newSolution().chunk(List.of(1, 2, 3, 4), 2))
        .containsExactly(List.of(1, 2), List.of(3, 4));
    assertThat(newSolution().chunk(List.of(1, 2, 3), 10)).containsExactly(List.of(1, 2, 3));
    assertThat(newSolution().chunk(List.of(1, 2, 3), 1))
        .containsExactly(List.of(1), List.of(2), List.of(3));
  }

  @Test
  @DisplayName("Пустой список и невалидный chunkSize")
  void chunkEdgeCases() {
    assertThat(newSolution().chunk(List.of(), 3)).isEmpty();
    assertThatThrownBy(() -> newSolution().chunk(List.of(1), 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().chunk(List.of(1), -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newSolution().chunk(null, 1)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("zipWithNext даёт соседние пары")
  void zipWithNextPairs() {
    assertThat(newSolution().zipWithNext(List.of(1, 2, 3, 4)))
        .containsExactly(new Pair<>(1, 2), new Pair<>(2, 3), new Pair<>(3, 4));
    assertThat(newSolution().zipWithNext(List.of("a", "b"))).containsExactly(new Pair<>("a", "b"));
  }

  @Test
  @DisplayName("zipWithNext на пустом и одноэлементном списке")
  void zipWithNextEmptyAndSingle() {
    assertThat(newSolution().zipWithNext(List.of())).isEmpty();
    assertThat(newSolution().zipWithNext(List.of(42))).isEmpty();
    assertThatThrownBy(() -> newSolution().zipWithNext(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Результаты и вложенные списки неизменяемы")
  void immutableResults() {
    List<List<Integer>> chunks = newSolution().chunk(List.of(1, 2, 3), 2);
    List<Pair<Integer, Integer>> pairs = newSolution().zipWithNext(List.of(1, 2));

    assertThatThrownBy(() -> chunks.add(List.of(4)))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> chunks.get(0).add(9))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> pairs.add(new Pair<>(3, 4)))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
