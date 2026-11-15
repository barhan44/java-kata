package dev.kata.solutions.distributed;

import dev.kata.distributed.Kata22_RaftLeaderElection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Эталонное решение Kata 22.
 *
 * <h2>Переходы состояний</h2>
 *
 * <pre>
 *   FOLLOWER --election timeout--> CANDIDATE (term++, votedFor=self)
 *   CANDIDATE --majority votes--> LEADER    (+ heartbeats)
 *   CANDIDATE --higher term-----> FOLLOWER
 *   LEADER    --higher term-----> FOLLOWER
 *   LEADER    --election timeout--> LEADER  (игнорируется: лидер шлёт heartbeat'ы)
 * </pre>
 *
 * <h2>Голосование и Log Up-to-Date</h2>
 *
 * <p>Голос отдаётся, если в текущем терме ещё никто не голосовал (или уже голосовали за этого же
 * кандидата) и журнал кандидата не менее свежий. Свежесть сравнивается лексикографически: {@code
 * lastLogTerm}, затем {@code lastLogIndex}. Кандидат с более высоким последним термом побеждает
 * независимо от длины; при равных термах побеждает более длинный журнал. Это ключевая гарантия
 * Raft: лидером не станет узел, пропустивший больше записей.
 *
 * <p>Запрос с меньшим термом отклоняется ответом с текущим термом, но состояние не меняет. Запрос с
 * большим термом сначала переводит узел в follower'а (сбрасывая {@code votedFor}/{@code leaderId}),
 * после чего голос может быть отдан.
 *
 * <h2>Выборы и split-vote</h2>
 *
 * <p>Кандидат добавляет свой голос в {@link #votesGranted} и рассылает запросы всем пирам. Голоса
 * учитываются как множество: повторный ответ того же узла не приближает победу. При {@code
 * votesGranted.size() >= clusterSize / 2 + 1} узел становится лидером и возвращает heartbeat'ы всем
 * пирам. Если голосов не хватило, следующий {@code onElectionTimeout} увеличивает терм и начинает
 * новые выборы — так разрешается split-vote.
 *
 * <h2>Heartbeat</h2>
 *
 * <p>{@code AppendEntriesReq} с {@code term >= currentTerm} подтверждает лидера: узел становится
 * follower'ом, запоминает {@code leaderId} и отвечает success. Более старый heartbeat отклоняется с
 * текущим термом. Сброс election-таймера не входит в автомат: реальное время — ответственность
 * вызывающего (в тестах таймаут — явное событие).
 */
public final class Kata22_RaftLeaderElectionSolution extends Kata22_RaftLeaderElection {

  public Kata22_RaftLeaderElectionSolution(int nodeId, int clusterSize) {
    super(nodeId, clusterSize);
    if (clusterSize <= 0) {
      throw new IllegalArgumentException("clusterSize must be > 0, got " + clusterSize);
    }
    if (nodeId < 0 || nodeId >= clusterSize) {
      throw new IllegalArgumentException(
          "nodeId must be in [0, " + clusterSize + "), got " + nodeId);
    }
  }

  @Override
  public List<Message> onElectionTimeout() {
    if (role == Role.LEADER) {
      return List.of();
    }

    currentTerm++;
    role = Role.CANDIDATE;
    votedFor = nodeId;
    leaderId = -1;
    votesGranted.clear();
    votesGranted.add(nodeId);

    if (clusterSize == 1) {
      return becomeLeader();
    }

    List<Message> requests = new ArrayList<>(clusterSize - 1);
    long lastIndex = lastLogIndex();
    long lastTerm = lastLogTerm();
    for (int peer = 0; peer < clusterSize; peer++) {
      if (peer != nodeId) {
        requests.add(new RequestVoteReq(nodeId, peer, currentTerm, lastIndex, lastTerm));
      }
    }
    return requests;
  }

  @Override
  public List<Message> onRequestVote(RequestVoteReq request) {
    Objects.requireNonNull(request, "request");

    if (request.term() < currentTerm) {
      return List.of(new RequestVoteResp(nodeId, request.from(), currentTerm, false));
    }
    if (request.term() > currentTerm) {
      becomeFollower(request.term());
    }

    boolean upToDate =
        request.lastLogTerm() > lastLogTerm()
            || (request.lastLogTerm() == lastLogTerm() && request.lastLogIndex() >= lastLogIndex());
    boolean canVote = votedFor == -1 || votedFor == request.from();
    boolean granted = canVote && upToDate;
    if (granted) {
      votedFor = request.from();
    }

    return List.of(new RequestVoteResp(nodeId, request.from(), currentTerm, granted));
  }

  @Override
  public List<Message> onRequestVoteResp(RequestVoteResp response) {
    Objects.requireNonNull(response, "response");

    if (response.term() > currentTerm) {
      becomeFollower(response.term());
      return List.of();
    }
    if (response.term() < currentTerm || role != Role.CANDIDATE) {
      return List.of();
    }
    if (!response.granted()
        || response.from() < 0
        || response.from() >= clusterSize
        || response.from() == nodeId) {
      return List.of();
    }

    votesGranted.add(response.from());
    if (votesGranted.size() >= majority()) {
      return becomeLeader();
    }
    return List.of();
  }

  @Override
  public List<Message> onHeartbeat(AppendEntriesReq request) {
    Objects.requireNonNull(request, "request");

    if (request.term() < currentTerm) {
      return List.of(new AppendEntriesResp(nodeId, request.from(), currentTerm, false));
    }

    if (request.term() > currentTerm) {
      becomeFollower(request.term());
    } else if (role != Role.FOLLOWER) {
      // В одном терме не может быть двух лидеров; heartbeat — авторитетное доказательство,
      // что выборы завершились не в нашу пользу.
      role = Role.FOLLOWER;
      votesGranted.clear();
    }

    leaderId = request.from();
    return List.of(new AppendEntriesResp(nodeId, request.from(), currentTerm, true));
  }

  @Override
  public void appendLogEntry(long term) {
    if (term <= 0L) {
      throw new IllegalArgumentException("term must be > 0, got " + term);
    }
    log.add(term);
  }

  private List<Message> becomeLeader() {
    role = Role.LEADER;
    leaderId = nodeId;
    votesGranted.clear();

    List<Message> heartbeats = new ArrayList<>(Math.max(0, clusterSize - 1));
    for (int peer = 0; peer < clusterSize; peer++) {
      if (peer != nodeId) {
        heartbeats.add(new AppendEntriesReq(nodeId, peer, currentTerm));
      }
    }
    return heartbeats;
  }

  private void becomeFollower(long term) {
    currentTerm = term;
    role = Role.FOLLOWER;
    votedFor = -1;
    leaderId = -1;
    votesGranted.clear();
  }

  private int majority() {
    return clusterSize / 2 + 1;
  }
}
