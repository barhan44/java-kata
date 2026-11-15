package dev.kata.distributed;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kata 22. Raft Leader Election: детерминированный конечный автомат без сети.
 *
 * <h2>Инженерный контекст</h2>
 *
 * <p>Raft — протокол консенсуса, на котором построены etcd, Consul, CockroachDB. Leader election —
 * его первая фаза: узлы переходят между {@code FOLLOWER}, {@code CANDIDATE} и {@code LEADER},
 * голосуют за кандидатов и выбирают лидера большинством. Здесь реализована <b>только логика</b>:
 * никаких сокетов, таймеров и потоков. События ({@code onElectionTimeout}, получение сообщений)
 * передаются методами, а возвращаемые сообщения тест/симулятор доставляет сам. Это позволяет
 * детерминированно проверить гонки и split-vote.
 *
 * <h2>Термы и голоса</h2>
 *
 * <ul>
 *   <li>Каждый узел хранит {@code currentTerm}, {@code votedFor} и роль. Терм — монотонное
 *       логическое время; при виде сообщения с большим термом любой узел (включая лидера)
 *       немедленно становится follower'ом и обновляет терм.
 *   <li>Узел голосует не более одного раза за терм. Голос отдаётся, только если кандидат не менее
 *       «свежий»: сравнение сначала по последнему терму записи журнала, затем по длине журнала
 *       (Raft Log Up-to-Date rule).
 *   <li>Кандидат получает голос за себя и рассылает {@link RequestVoteReq}. Набрав {@code
 *       clusterSize / 2 + 1} голосов, становится лидером и возвращает heartbeat'ы.
 *   <li>Heartbeat ({@link AppendEntriesReq}) с термом не меньше текущего подтверждает лидера и
 *       сбрасывает кандидата/лидера в follower'а. Меньший терм отклоняется с текущим термом.
 * </ul>
 *
 * <h2>Контракт событий</h2>
 *
 * <ol>
 *   <li>{@link #onElectionTimeout()} вызывается таймером. Follower/candidate увеличивает терм,
 *       голосует за себя и возвращает запросы всем пирам. Leader игнорирует таймаут.
 *   <li>{@link #onRequestVote(RequestVoteReq)} возвращает {@link RequestVoteResp}. Ответ адресован
 *       {@code req.from()}.
 *   <li>{@link #onRequestVoteResp(RequestVoteResp)} учитывает голос; при большинстве возвращает
 *       heartbeat'ы всем пирам.
 *   <li>{@link #onHeartbeat(AppendEntriesReq)} возвращает {@link AppendEntriesResp} и обновляет
 *       {@code leaderId}. Сброс election-таймера — ответственность вызывающего, у автомата нет
 *       реального времени.
 * </ol>
 *
 * <p>{@link #appendLogEntry(long)} — тестовый/симуляционный помощник, позволяющий подготовить
 * журналы кандидатов и избирателей для проверки up-to-date правила. В настоящем Raft записи
 * добавляются лидером и реплицируются через AppendEntries.
 *
 * <h2>Сложность</h2>
 *
 * <ul>
 *   <li>Обработка сообщения — O(email: clusterSize) на рассылку; проверка голоса — O(1).
 *   <li>Память — O(размера журнала + clusterSize) на узел.
 * </ul>
 *
 * <h2>Граничные случаи</h2>
 *
 * <ul>
 *   <li>{@code nodeId} должен быть в {@code [0, clusterSize)}, {@code clusterSize > 0}.
 *   <li>{@code null} сообщение — {@link NullPointerException}.
 *   <li>Кластер из одного узла: таймаут сразу делает его лидером, сообщений нет.
 *   <li>Повторные/чужие голоса не считаются дважды; {@code votesGranted} — множество.
 *   <li>Термы в логах — положительные; последний терм пустого журнала — 0.
 * </ul>
 *
 * <p>Реализацию см. в {@code dev.kata.solutions.distributed.Kata22_RaftLeaderElectionSolution}.
 * Тесты: {@code Kata22_RaftLeaderElectionTest}.
 */
public class Kata22_RaftLeaderElection {

  /** Роль узла. */
  public enum Role {
    FOLLOWER,
    CANDIDATE,
    LEADER
  }

  /** Сообщение Raft election. */
  public sealed interface Message
      permits RequestVoteReq, RequestVoteResp, AppendEntriesReq, AppendEntriesResp {}

  /** Запрос голоса. */
  public record RequestVoteReq(int from, int to, long term, long lastLogIndex, long lastLogTerm)
      implements Message {}

  /** Ответ на запрос голоса. */
  public record RequestVoteResp(int from, int to, long term, boolean granted) implements Message {}

  /** Heartbeat/AppendEntries (в этой кате — только терм и лидер). */
  public record AppendEntriesReq(int from, int to, long term) implements Message {}

  /** Ответ на heartbeat. */
  public record AppendEntriesResp(int from, int to, long term, boolean success)
      implements Message {}

  protected final int nodeId;
  protected final int clusterSize;

  protected Role role = Role.FOLLOWER;
  protected long currentTerm;
  protected int votedFor = -1;
  protected int leaderId = -1;
  protected final List<Long> log = new ArrayList<>();
  protected final Set<Integer> votesGranted = new HashSet<>();

  /**
   * @param nodeId идентификатор узла в {@code [0, clusterSize)}
   * @param clusterSize количество узлов; больше нуля
   */
  public Kata22_RaftLeaderElection(int nodeId, int clusterSize) {
    this.nodeId = nodeId;
    this.clusterSize = clusterSize;
  }

  /**
   * Обрабатывает истечение election timeout.
   *
   * @return запросы голоса пирам либо heartbeat'ы, если кластер из одного узла
   */
  public List<Message> onElectionTimeout() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Обрабатывает запрос голоса.
   *
   * @return ответ ровно одному кандидату
   */
  public List<Message> onRequestVote(RequestVoteReq request) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Обрабатывает ответ на запрос голоса.
   *
   * @return heartbeat'ы, если этот узел только что стал лидером; иначе пустой список
   */
  public List<Message> onRequestVoteResp(RequestVoteResp response) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Обрабатывает heartbeat лидера.
   *
   * @return ответ с текущим термом и признаком успеха
   */
  public List<Message> onHeartbeat(AppendEntriesReq request) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Добавляет терм в локальный журнал (симуляция записей лидера).
   *
   * @param term терм записи; больше нуля
   */
  public void appendLogEntry(long term) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public Role role() {
    return role;
  }

  public long currentTerm() {
    return currentTerm;
  }

  /**
   * @return id кандидата, за которого голосовали в текущем терме, или -1
   */
  public int votedFor() {
    return votedFor;
  }

  /**
   * @return известный лидер или -1
   */
  public int leaderId() {
    return leaderId;
  }

  public int nodeId() {
    return nodeId;
  }

  public int clusterSize() {
    return clusterSize;
  }

  public long lastLogIndex() {
    return log.size();
  }

  public long lastLogTerm() {
    return log.isEmpty() ? 0L : log.get(log.size() - 1);
  }

  /**
   * @return неизменяемая копия термов журнала
   */
  public List<Long> logTerms() {
    return List.copyOf(log);
  }
}
