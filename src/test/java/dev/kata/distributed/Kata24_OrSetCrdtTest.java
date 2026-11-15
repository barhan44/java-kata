package dev.kata.distributed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.distributed.Kata24_OrSetCrdtSolution;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 24.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata24* -DuseSolution=true</pre>
 */
@DisplayName("Kata 24: OR-Set CRDT с Add-Wins семантикой")
class Kata24_OrSetCrdtTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private <T> Kata24_OrSetCrdt<T> newReplica(String replicaId) {
    return USE_SOLUTION
        ? new Kata24_OrSetCrdtSolution<>(replicaId)
        : new Kata24_OrSetCrdt<>(replicaId);
  }

  @Test
  @DisplayName("add/remove/contains на одной реплике")
  void addRemoveContains() {
    Kata24_OrSetCrdt<String> replica = newReplica("A");

    replica.add("x");
    assertThat(replica.contains("x")).isTrue();
    assertThat(replica.elements()).containsExactly("x");

    replica.remove("x");
    assertThat(replica.contains("x")).isFalse();
    assertThat(replica.elements()).isEmpty();
  }

  @Test
  @DisplayName("Повторное добавление после удаления создаёт новый тег")
  void readdAfterRemove() {
    Kata24_OrSetCrdt<String> replica = newReplica("A");

    replica.add("x");
    replica.remove("x");
    assertThat(replica.contains("x")).isFalse();

    replica.add("x");
    assertThat(replica.contains("x")).isTrue();
    assertThat(replica.elements()).containsExactly("x");
  }

  @Test
  @DisplayName("remove отсутствующего элемента — no-op")
  void removeMissingIsNoOp() {
    Kata24_OrSetCrdt<String> replica = newReplica("A");

    replica.remove("ghost");

    assertThat(replica.contains("ghost")).isFalse();
    assertThat(replica.elements()).isEmpty();
  }

  @Test
  @DisplayName("Конкурентные add и remove: добавление побеждает")
  void concurrentAddWinsOverRemove() {
    Kata24_OrSetCrdt<String> a = newReplica("A");
    Kata24_OrSetCrdt<String> b = newReplica("B");

    a.add("x");
    a.remove("x"); // A наблюдает и удаляет свой тег
    b.add("x"); // B добавляет конкурентно, не видя удаления A

    a.merge(b);
    assertThat(a.contains("x")).isTrue();
    assertThat(a.elements()).containsExactly("x");

    b.merge(a);
    assertThat(b.contains("x")).isTrue();
    assertThat(b.elements()).containsExactly("x");
  }

  @Test
  @DisplayName("Если обе реплики удалили все наблюдаемые теги, элемент исчезает")
  void bothReplicasRemovingObservedTagsWins() {
    Kata24_OrSetCrdt<String> a = newReplica("A");
    Kata24_OrSetCrdt<String> b = newReplica("B");

    a.add("x");
    b.add("x");
    a.merge(b);
    b.merge(a);

    a.remove("x");
    b.remove("x");
    a.merge(b);
    b.merge(a);

    assertThat(a.contains("x")).isFalse();
    assertThat(b.contains("x")).isFalse();
    assertThat(a.elements()).isEmpty();
    assertThat(b.elements()).isEmpty();
  }

  @Test
  @DisplayName("Удаление на реплике, не наблюдавшей добавление, ничего не удаляет")
  void removeWithoutObservationIsNoOp() {
    Kata24_OrSetCrdt<String> a = newReplica("A");
    Kata24_OrSetCrdt<String> b = newReplica("B");

    a.add("x");
    b.remove("x"); // B не видел тег A1

    b.merge(a);
    assertThat(b.contains("x")).isTrue();

    a.merge(b);
    assertThat(a.contains("x")).isTrue();
  }

  @Test
  @DisplayName("Re-add после распределённого удаления: новый тег остаётся живым")
  void readdAfterDistributedRemoval() {
    Kata24_OrSetCrdt<String> a = newReplica("A");
    Kata24_OrSetCrdt<String> b = newReplica("B");

    a.add("x");
    b.merge(a);
    a.remove("x");
    b.remove("x");
    a.add("x"); // новый тег A2

    a.merge(b);
    b.merge(a);

    assertThat(a.contains("x")).isTrue();
    assertThat(b.contains("x")).isTrue();
  }

  @Test
  @DisplayName("Слияние идемпотентно, включая слияние с самим собой")
  void mergeIsIdempotent() {
    Kata24_OrSetCrdt<String> a = newReplica("A");
    Kata24_OrSetCrdt<String> b = newReplica("B");
    a.add("x");
    b.add("y");

    a.merge(b);
    Set<String> once = a.elements();
    a.merge(b);
    a.merge(b);
    a.merge(a);

    assertThat(a.elements()).isEqualTo(once);
  }

  @Test
  @DisplayName("Слияние коммутативно")
  void mergeIsCommutative() {
    Consumer<Kata24_OrSetCrdt<String>> opsA =
        replica -> {
          replica.add("a");
          replica.add("b");
          replica.remove("a");
          replica.add("c");
        };
    Consumer<Kata24_OrSetCrdt<String>> opsB =
        replica -> {
          replica.add("b");
          replica.add("d");
          replica.remove("b");
          replica.add("a");
        };

    Kata24_OrSetCrdt<String> a1 = newReplica("A");
    Kata24_OrSetCrdt<String> b1 = newReplica("B");
    Kata24_OrSetCrdt<String> a2 = newReplica("A");
    Kata24_OrSetCrdt<String> b2 = newReplica("B");
    opsA.accept(a1);
    opsB.accept(b1);
    opsA.accept(a2);
    opsB.accept(b2);

    a1.merge(b1);
    b2.merge(a2);

    assertThat(a1.elements()).isEqualTo(b2.elements());
  }

  @Test
  @DisplayName("Слияние ассоциативно")
  void mergeIsAssociative() {
    Consumer<Kata24_OrSetCrdt<String>> opsA =
        replica -> {
          replica.add("a");
          replica.add("b");
          replica.remove("a");
        };
    Consumer<Kata24_OrSetCrdt<String>> opsB =
        replica -> {
          replica.add("b");
          replica.add("c");
          replica.remove("b");
        };
    Consumer<Kata24_OrSetCrdt<String>> opsC =
        replica -> {
          replica.add("c");
          replica.add("d");
          replica.remove("c");
        };

    Kata24_OrSetCrdt<String> a1 = newReplica("A");
    Kata24_OrSetCrdt<String> b1 = newReplica("B");
    Kata24_OrSetCrdt<String> c1 = newReplica("C");
    Kata24_OrSetCrdt<String> a2 = newReplica("A");
    Kata24_OrSetCrdt<String> b2 = newReplica("B");
    Kata24_OrSetCrdt<String> c2 = newReplica("C");
    opsA.accept(a1);
    opsB.accept(b1);
    opsC.accept(c1);
    opsA.accept(a2);
    opsB.accept(b2);
    opsC.accept(c2);

    a1.merge(b1);
    a1.merge(c1);

    b2.merge(c2);
    a2.merge(b2);

    assertThat(a1.elements()).isEqualTo(a2.elements());
  }

  @Test
  @DisplayName("Случайные операции на двух репликах сходятся после merge")
  void randomReplicasConverge() {
    Kata24_OrSetCrdt<String> a = newReplica("A");
    Kata24_OrSetCrdt<String> b = newReplica("B");
    Random random = new Random(42);

    for (int i = 0; i < 500; i++) {
      String element = "e" + random.nextInt(6);
      if (random.nextBoolean()) {
        a.add(element);
      } else {
        a.remove(element);
      }
    }
    for (int i = 0; i < 500; i++) {
      String element = "e" + random.nextInt(6);
      if (random.nextBoolean()) {
        b.add(element);
      } else {
        b.remove(element);
      }
    }

    a.merge(b);
    b.merge(a);

    assertThat(a.elements()).isEqualTo(b.elements());
    for (int i = 0; i < 6; i++) {
      String element = "e" + i;
      assertThat(a.contains(element)).isEqualTo(b.contains(element));
    }
  }

  @Test
  @DisplayName("Элемент null поддерживается")
  void nullElementIsSupported() {
    Kata24_OrSetCrdt<String> replica = newReplica("A");

    replica.add(null);
    assertThat(replica.contains(null)).isTrue();
    assertThat(replica.elements()).containsNull();

    replica.remove(null);
    assertThat(replica.contains(null)).isFalse();
    assertThat(replica.elements()).isEmpty();
  }

  @Test
  @DisplayName("elements() возвращает неизменяемый снимок")
  void elementsAreImmutableSnapshot() {
    Kata24_OrSetCrdt<String> replica = newReplica("A");
    replica.add("x");

    Set<String> snapshot = replica.elements();
    replica.add("y");

    assertThat(snapshot).containsExactly("x").doesNotContain("y");
    assertThatThrownBy(() -> snapshot.add("z")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Валидация replicaId и аргументов merge")
  void validation() {
    assertThatThrownBy(() -> newReplica(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> newReplica("")).isInstanceOf(IllegalArgumentException.class);

    Kata24_OrSetCrdt<String> replica = newReplica("A");
    assertThatThrownBy(() -> replica.merge(null)).isInstanceOf(NullPointerException.class);
  }
}
