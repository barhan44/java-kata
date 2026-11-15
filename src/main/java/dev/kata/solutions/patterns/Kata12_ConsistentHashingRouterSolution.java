package dev.kata.solutions.patterns;

import dev.kata.patterns.Kata12_ConsistentHashingRouter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Эталонное решение Kata 12.
 *
 * <h2>Хеш-функция</h2>
 *
 * <p>Используется FNV-1a 64-бит с финализатором MurmurHash3 ({@code fmix64}). FNV даёт хорошее
 * лавинное перемешивание за один проход по строке и не требует аллокаций; финализатор исправляет
 * слабое перемешивание старших битов у коротких ключей (у чистого FNV младшие биты зависят только
 * от последних символов). {@link String#hashCode()} не подходит: 32 бита и линейная структура
 * {@code h = 31*h + c} дают на кольце выраженные кластеры.
 *
 * <p>Строка хешируется по UTF-16 code units, без {@code getBytes(UTF_8)}: конверсия в байты
 * аллоцировала бы массив на каждый вызов {@code route}. Два байта каждого {@code char}
 * обрабатываются по очереди; для маршрутизации важна стабильность и равномерность, а не
 * совместимость с байтовым представлением.
 *
 * <h2>Кольцо без боксинга</h2>
 *
 * <p>Наивное {@code TreeMap<Long, Node>} на каждом {@code route} создаёт {@link Long} для
 * автобоксинга ключа поиска. Поэтому топология хранится в двух параллельных массивах: {@code long[]
 * hashes} (отсортирован) и {@code NodeSlot[] slots}. Поиск — {@link Arrays#binarySearch(long[],
 * long)}, O(log V), ноль аллокаций. {@code TreeMap} используется только при построении снимка:
 * обновления топологии редки, а боксить там можно.
 *
 * <h2>Иммутабельный снимок и copy-on-write</h2>
 *
 * <p>Поле {@code snapshot} объявлено {@code volatile}. Обновления берут блокировку, строят новый
 * {@link Snapshot} и публикуют его одной записью. Читатели не блокируются и не видят частично
 * обновлённое кольцо: массив и слоты в снимке никогда не мутируются после публикации. Это
 * классический copy-on-write: дороже на запись, идеально на чтение.
 *
 * <h2>Виртуальные ноды и минимальная миграция</h2>
 *
 * <p>Хеш виртуальной ноды вычисляется как {@code fmix64(hash(nodeId) + GOLDEN_GAMMA * (i + 1))}.
 * Последовательность Вейля (шаг — золотое сечение в 64-битной арифметике) равномерно разносит
 * реплики по кольцу без хранения состояния. При добавлении узла его точки занимают случайные дуги,
 * и переезжает только {@code ~1/(N+1)} ключей; при удалении — только ключи этого узла. Алгоритм не
 * пересчитывает хеши существующих нод, поэтому это свойство выполняется точно, а не приблизительно.
 *
 * <h2>Коллизии</h2>
 *
 * <p>Два узла теоретически могут дать одинаковый хеш виртуальной ноды. При построении снимка
 * коллизия разрешается детерминированным «перехешированием» с шагом {@code GOLDEN_GAMMA}, пока не
 * найдётся свободная точка. На практике при 64-битном хеше и тысячах нод это событие пренебрежимо
 * редкое.
 *
 * <h2>Кэш {@link Optional}</h2>
 *
 * <p>{@code Optional.of(nodeId)} создаётся один раз на физический узел при обновлении топологии и
 * переиспользуется всеми его виртуальными нодами. Поэтому {@code route} не аллоцирует даже обёртку
 * — это важно для JMH-метрики {@code gc.alloc.rate.norm}.
 */
public final class Kata12_ConsistentHashingRouterSolution extends Kata12_ConsistentHashingRouter {

  private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
  private static final long FNV_PRIME = 0x100000001b3L;
  private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;

  private static final long[] EMPTY_HASHES = new long[0];
  private static final NodeSlot[] EMPTY_SLOTS = new NodeSlot[0];

  private final Object updateLock = new Object();
  private volatile Snapshot snapshot = Snapshot.empty();

  public Kata12_ConsistentHashingRouterSolution(int virtualNodesCount) {
    super(virtualNodesCount);
    if (virtualNodesCount <= 0) {
      throw new IllegalArgumentException("virtualNodesCount must be > 0, got " + virtualNodesCount);
    }
  }

  @Override
  public void addNode(String nodeId) {
    Objects.requireNonNull(nodeId, "nodeId");
    synchronized (updateLock) {
      Snapshot current = snapshot;
      if (current.nodes.contains(nodeId)) {
        return;
      }
      Map<String, NodeSlot> slotsByNode = new HashMap<>(current.slotsByNode);
      slotsByNode.put(nodeId, new NodeSlot(nodeId, Optional.of(nodeId)));
      publish(slotsByNode);
    }
  }

  @Override
  public void removeNode(String nodeId) {
    Objects.requireNonNull(nodeId, "nodeId");
    synchronized (updateLock) {
      Snapshot current = snapshot;
      if (!current.nodes.contains(nodeId)) {
        return;
      }
      Map<String, NodeSlot> slotsByNode = new HashMap<>(current.slotsByNode);
      slotsByNode.remove(nodeId);
      publish(slotsByNode);
    }
  }

  @Override
  public Optional<String> route(String key) {
    Objects.requireNonNull(key, "key");
    Snapshot current = snapshot;
    long[] hashes = current.hashes;
    if (hashes.length == 0) {
      return Optional.empty();
    }

    long hash = hash64(key);
    int index = Arrays.binarySearch(hashes, hash);
    if (index < 0) {
      index = -index - 1; // insertion point: первый узел по часовой стрелке
    }
    if (index == hashes.length) {
      index = 0; // ключ больше всех точек — замыкаем кольцо
    }
    return current.slots[index].routed();
  }

  @Override
  public Set<String> nodes() {
    return snapshot.nodes;
  }

  /** Строит и публикует новое кольцо. Вызывается только под {@link #updateLock}. */
  private void publish(Map<String, NodeSlot> slotsByNode) {
    TreeMap<Long, NodeSlot> ring = new TreeMap<>();
    // Детерминированный порядок обхода: при редкой коллизии хешей результат не зависит от
    // итерационного порядка HashMap и остаётся стабильным между перестроениями кольца.
    for (String nodeId : new TreeSet<>(slotsByNode.keySet())) {
      NodeSlot slot = slotsByNode.get(nodeId);
      long base = hash64(nodeId);
      for (int replica = 0; replica < virtualNodesCount; replica++) {
        long hash = fmix64(base + GOLDEN_GAMMA * (replica + 1L));
        while (ring.containsKey(hash)) {
          hash = fmix64(hash + GOLDEN_GAMMA);
        }
        ring.put(hash, slot);
      }
    }

    long[] hashes = new long[ring.size()];
    NodeSlot[] slots = new NodeSlot[ring.size()];
    int index = 0;
    for (Map.Entry<Long, NodeSlot> entry : ring.entrySet()) {
      hashes[index] = entry.getKey();
      slots[index] = entry.getValue();
      index++;
    }

    snapshot =
        new Snapshot(hashes, slots, Map.copyOf(slotsByNode), Set.copyOf(slotsByNode.keySet()));
  }

  /** FNV-1a 64 по UTF-16 code units + финализатор MurmurHash3. */
  private static long hash64(String value) {
    long hash = FNV_OFFSET_BASIS;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      hash ^= c & 0xFF;
      hash *= FNV_PRIME;
      hash ^= (c >>> 8) & 0xFF;
      hash *= FNV_PRIME;
    }
    return fmix64(hash);
  }

  /** Финальный миксер MurmurHash3: лавинное перемешивание всех 64 бит. */
  private static long fmix64(long value) {
    value ^= value >>> 33;
    value *= 0xff51afd7ed558ccdL;
    value ^= value >>> 33;
    value *= 0xc4ceb9fe1a85ec53L;
    value ^= value >>> 33;
    return value;
  }

  /**
   * Иммутабельный снимок топологии.
   *
   * @param hashes отсортированные хеши виртуальных нод
   * @param slots слоты в том же порядке, что и {@code hashes}
   * @param slotsByNode слоты по id узла — для построения следующего снимка
   * @param nodes неизменяемое множество узлов
   */
  private record Snapshot(
      long[] hashes, NodeSlot[] slots, Map<String, NodeSlot> slotsByNode, Set<String> nodes) {

    static Snapshot empty() {
      return new Snapshot(EMPTY_HASHES, EMPTY_SLOTS, Map.of(), Set.of());
    }
  }

  /** Узел и заранее созданный ответ {@code route} для него. */
  private record NodeSlot(String nodeId, Optional<String> routed) {}
}
