package dev.kata.distributed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.distributed.Kata22_RaftLeaderElection.AppendEntriesReq;
import dev.kata.distributed.Kata22_RaftLeaderElection.AppendEntriesResp;
import dev.kata.distributed.Kata22_RaftLeaderElection.Message;
import dev.kata.distributed.Kata22_RaftLeaderElection.RequestVoteReq;
import dev.kata.distributed.Kata22_RaftLeaderElection.RequestVoteResp;
import dev.kata.distributed.Kata22_RaftLeaderElection.Role;
import dev.kata.solutions.distributed.Kata22_RaftLeaderElectionSolution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 22.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata22* -DuseSolution=true</pre>
 */
@DisplayName("Kata 22: Raft Leader Election")
class Kata22_RaftLeaderElectionTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata22_RaftLeaderElection newNode(int nodeId, int clusterSize) {
    return USE_SOLUTION
        ? new Kata22_RaftLeaderElectionSolution(nodeId, clusterSize)
        : new Kata22_RaftLeaderElection(nodeId, clusterSize);
  }

  @Test
  @DisplayName("Кластер из одного узла: таймаут сразу делает лидером")
  void singleNodeBecomesLeader() {
    Kata22_RaftLeaderElection node = newNode(0, 1);

    assertThat(node.onElectionTimeout()).isEmpty();

    assertThat(node.role()).isEqualTo(Role.LEADER);
    assertThat(node.currentTerm()).isEqualTo(1L);
    assertThat(node.votedFor()).isZero();
    assertThat(node.leaderId()).isZero();
  }

  @Test
  @DisplayName("Выборы в кластере из трёх узлов: кандидат получает большинство")
  void threeNodeElection() {
    Map<Integer, Kata22_RaftLeaderElection> nodes = cluster(3);
    Kata22_RaftLeaderElection leader = nodes.get(0);

    List<Message> requests = leader.onElectionTimeout();
    assertThat(requests).hasSize(2);
    assertThat(requests)
        .allSatisfy(
            message -> {
              assertThat(message).isInstanceOf(RequestVoteReq.class);
              RequestVoteReq request = (RequestVoteReq) message;
              assertThat(request.from()).isZero();
              assertThat(request.term()).isEqualTo(1L);
              assertThat(request.lastLogIndex()).isZero();
              assertThat(request.lastLogTerm()).isZero();
            });

    List<Message> responses = deliver(nodes, requests);
    assertThat(responses).hasSize(2);
    assertThat(responses)
        .allSatisfy(
            message -> {
              RequestVoteResp response = (RequestVoteResp) message;
              assertThat(response.granted()).isTrue();
              assertThat(response.term()).isEqualTo(1L);
            });
    assertThat(nodes.get(1).role()).isEqualTo(Role.FOLLOWER);
    assertThat(nodes.get(1).votedFor()).isZero();
    assertThat(nodes.get(2).votedFor()).isZero();

    List<Message> heartbeats = deliver(nodes, responses);
    assertThat(heartbeats).isNotEmpty();
    assertThat(heartbeats)
        .allSatisfy(message -> assertThat(message).isInstanceOf(AppendEntriesReq.class));
    deliver(nodes, heartbeats);

    assertThat(leader.role()).isEqualTo(Role.LEADER);
    assertThat(leader.currentTerm()).isEqualTo(1L);
    assertThat(leader.leaderId()).isZero();
    assertThat(nodes.get(1).leaderId()).isZero();
    assertThat(nodes.get(2).leaderId()).isZero();
  }

  @Test
  @DisplayName("Кластер из пяти узлов: лидер появляется после третьего голоса")
  void fiveNodeElectionNeedsMajority() {
    Map<Integer, Kata22_RaftLeaderElection> nodes = cluster(5);
    Kata22_RaftLeaderElection candidate = nodes.get(0);

    List<Message> requests = candidate.onElectionTimeout();
    List<Message> responses = deliver(nodes, requests);
    assertThat(responses).hasSize(4);

    deliver(nodes, List.of(responses.get(0)));
    assertThat(candidate.role()).isEqualTo(Role.CANDIDATE);
    assertThat(candidate.currentTerm()).isEqualTo(1L);

    deliver(nodes, List.of(responses.get(1)));
    assertThat(candidate.role()).isEqualTo(Role.LEADER);
    assertThat(candidate.leaderId()).isZero();
  }

  @Test
  @DisplayName("Split-vote: два кандидата по 2 голоса, перевыборы в следующем терме")
  void splitVoteThenReelection() {
    Map<Integer, Kata22_RaftLeaderElection> nodes = cluster(4);
    Kata22_RaftLeaderElection a = nodes.get(0);
    Kata22_RaftLeaderElection b = nodes.get(1);

    List<Message> aRequests = a.onElectionTimeout();
    List<Message> bRequests = b.onElectionTimeout();

    // Моделируем задержку сети: A успевает попросить голос только у C, B — только у D.
    deliver(nodes, deliver(nodes, onlyTo(aRequests, 2)));
    deliver(nodes, deliver(nodes, onlyTo(bRequests, 3)));

    assertThat(a.role()).isEqualTo(Role.CANDIDATE);
    assertThat(b.role()).isEqualTo(Role.CANDIDATE);
    assertThat(a.currentTerm()).isEqualTo(1L);
    assertThat(b.currentTerm()).isEqualTo(1L);
    assertThat(a.leaderId()).isEqualTo(-1);
    assertThat(b.leaderId()).isEqualTo(-1);

    List<Message> secondRound = a.onElectionTimeout();
    assertThat(a.currentTerm()).isEqualTo(2L);
    List<Message> responses = deliver(nodes, secondRound);
    deliver(nodes, responses);

    assertThat(a.role()).isEqualTo(Role.LEADER);
    assertThat(a.currentTerm()).isEqualTo(2L);
    assertThat(b.role()).isEqualTo(Role.FOLLOWER);
    assertThat(b.currentTerm()).isEqualTo(2L);
  }

  @Test
  @DisplayName("Кандидат с устаревшим журналом не получает голос")
  void staleLogCandidateIsDenied() {
    Kata22_RaftLeaderElection voter = newNode(0, 3);
    voter.appendLogEntry(1L);
    voter.appendLogEntry(1L); // lastLogTerm=1, lastLogIndex=2

    List<Message> stale = voter.onRequestVote(new RequestVoteReq(1, 0, 1L, 0L, 0L));
    RequestVoteResp staleResponse = (RequestVoteResp) stale.get(0);
    assertThat(staleResponse.granted()).isFalse();
    assertThat(voter.votedFor()).isEqualTo(-1);

    List<Message> fresh = voter.onRequestVote(new RequestVoteReq(2, 0, 1L, 2L, 1L));
    RequestVoteResp freshResponse = (RequestVoteResp) fresh.get(0);
    assertThat(freshResponse.granted()).isTrue();
    assertThat(voter.votedFor()).isEqualTo(2);

    List<Message> duplicateTerm = voter.onRequestVote(new RequestVoteReq(1, 0, 1L, 5L, 1L));
    assertThat(((RequestVoteResp) duplicateTerm.get(0)).granted()).isFalse();

    List<Message> higherTerm = voter.onRequestVote(new RequestVoteReq(3, 0, 2L, 1L, 2L));
    assertThat(((RequestVoteResp) higherTerm.get(0)).granted()).isTrue();
    assertThat(voter.currentTerm()).isEqualTo(2L);
    assertThat(voter.votedFor()).isEqualTo(3);
  }

  @Test
  @DisplayName("Один голос на терм; новый терм сбрасывает votedFor")
  void voteOnlyOncePerTerm() {
    Kata22_RaftLeaderElection voter = newNode(0, 3);

    assertThat(
            ((RequestVoteResp) voter.onRequestVote(new RequestVoteReq(1, 0, 1L, 0L, 0L)).get(0))
                .granted())
        .isTrue();
    assertThat(
            ((RequestVoteResp) voter.onRequestVote(new RequestVoteReq(2, 0, 1L, 0L, 0L)).get(0))
                .granted())
        .isFalse();
    assertThat(voter.votedFor()).isEqualTo(1);

    assertThat(
            ((RequestVoteResp) voter.onRequestVote(new RequestVoteReq(2, 0, 2L, 0L, 0L)).get(0))
                .granted())
        .isTrue();
    assertThat(voter.currentTerm()).isEqualTo(2L);
    assertThat(voter.votedFor()).isEqualTo(2);
  }

  @Test
  @DisplayName("Heartbeat с более высоким термом смещает лидера")
  void higherTermHeartbeatStepsDownLeader() {
    Map<Integer, Kata22_RaftLeaderElection> nodes = electLeader(3, 0);
    Kata22_RaftLeaderElection oldLeader = nodes.get(0);
    assertThat(oldLeader.role()).isEqualTo(Role.LEADER);

    List<Message> response = oldLeader.onHeartbeat(new AppendEntriesReq(1, 0, 2L));

    AppendEntriesResp heartbeatResponse = (AppendEntriesResp) response.get(0);
    assertThat(heartbeatResponse.success()).isTrue();
    assertThat(heartbeatResponse.term()).isEqualTo(2L);
    assertThat(oldLeader.role()).isEqualTo(Role.FOLLOWER);
    assertThat(oldLeader.currentTerm()).isEqualTo(2L);
    assertThat(oldLeader.leaderId()).isEqualTo(1);
    assertThat(oldLeader.votedFor()).isEqualTo(-1);

    List<Message> stale = oldLeader.onHeartbeat(new AppendEntriesReq(1, 0, 1L));
    assertThat(((AppendEntriesResp) stale.get(0)).success()).isFalse();
    assertThat(oldLeader.currentTerm()).isEqualTo(2L);
    assertThat(oldLeader.leaderId()).isEqualTo(1);
  }

  @Test
  @DisplayName("Heartbeat того же терма останавливает кандидата")
  void heartbeatSameTermStopsCandidate() {
    Kata22_RaftLeaderElection candidate = newNode(0, 3);
    candidate.onElectionTimeout();
    assertThat(candidate.role()).isEqualTo(Role.CANDIDATE);

    List<Message> response = candidate.onHeartbeat(new AppendEntriesReq(1, 0, 1L));

    assertThat(((AppendEntriesResp) response.get(0)).success()).isTrue();
    assertThat(candidate.role()).isEqualTo(Role.FOLLOWER);
    assertThat(candidate.currentTerm()).isEqualTo(1L);
    assertThat(candidate.leaderId()).isEqualTo(1);
  }

  @Test
  @DisplayName("Ответ с более высоким термом сбрасывает кандидата")
  void higherTermVoteResponseStepsDownCandidate() {
    Kata22_RaftLeaderElection candidate = newNode(0, 3);
    candidate.onElectionTimeout();

    candidate.onRequestVoteResp(new RequestVoteResp(1, 0, 2L, true));

    assertThat(candidate.role()).isEqualTo(Role.FOLLOWER);
    assertThat(candidate.currentTerm()).isEqualTo(2L);
    assertThat(candidate.votedFor()).isEqualTo(-1);
    assertThat(candidate.leaderId()).isEqualTo(-1);
  }

  @Test
  @DisplayName("Лидер игнорирует election timeout")
  void leaderIgnoresElectionTimeout() {
    Map<Integer, Kata22_RaftLeaderElection> nodes = electLeader(3, 0);
    Kata22_RaftLeaderElection leader = nodes.get(0);
    long term = leader.currentTerm();

    assertThat(leader.onElectionTimeout()).isEmpty();

    assertThat(leader.role()).isEqualTo(Role.LEADER);
    assertThat(leader.currentTerm()).isEqualTo(term);
    assertThat(leader.leaderId()).isZero();
  }

  @Test
  @DisplayName("Повторные голоса одного узла не считаются дважды")
  void duplicateVotesDoNotCountTwice() {
    Kata22_RaftLeaderElection candidate = newNode(0, 4);
    candidate.onElectionTimeout();

    RequestVoteResp voteFromOne = new RequestVoteResp(1, 0, 1L, true);
    candidate.onRequestVoteResp(voteFromOne);
    candidate.onRequestVoteResp(voteFromOne);

    assertThat(candidate.role()).isEqualTo(Role.CANDIDATE);

    candidate.onRequestVoteResp(new RequestVoteResp(2, 0, 1L, true));
    assertThat(candidate.role()).isEqualTo(Role.LEADER);
    assertThat(candidate.currentTerm()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Запрос голоса содержит lastLogIndex и lastLogTerm кандидата")
  void voteRequestCarriesLogMetadata() {
    Kata22_RaftLeaderElection candidate = newNode(0, 3);
    candidate.appendLogEntry(1L);
    candidate.appendLogEntry(1L);
    candidate.appendLogEntry(2L);

    List<Message> requests = candidate.onElectionTimeout();

    assertThat(requests).hasSize(2);
    for (Message message : requests) {
      RequestVoteReq request = (RequestVoteReq) message;
      assertThat(request.lastLogIndex()).isEqualTo(3L);
      assertThat(request.lastLogTerm()).isEqualTo(2L);
    }
  }

  @Test
  @DisplayName("Журнал: добавление термов и неизменяемый снимок")
  void logEntries() {
    Kata22_RaftLeaderElection node = newNode(0, 3);
    assertThat(node.lastLogIndex()).isZero();
    assertThat(node.lastLogTerm()).isZero();

    node.appendLogEntry(1L);
    node.appendLogEntry(2L);

    assertThat(node.lastLogIndex()).isEqualTo(2L);
    assertThat(node.lastLogTerm()).isEqualTo(2L);
    assertThat(node.logTerms()).containsExactly(1L, 2L);
    assertThatThrownBy(() -> node.logTerms().add(3L))
        .isInstanceOf(UnsupportedOperationException.class);

    assertThatThrownBy(() -> node.appendLogEntry(0L)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> node.appendLogEntry(-1L)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Валидация конфигурации и null-сообщений")
  void validation() {
    assertThatThrownBy(() -> newNode(0, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newNode(0, -1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newNode(-1, 3)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> newNode(3, 3)).isInstanceOf(IllegalArgumentException.class);

    Kata22_RaftLeaderElection node = newNode(0, 3);
    assertThatThrownBy(() -> node.onRequestVote(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> node.onRequestVoteResp(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> node.onHeartbeat(null)).isInstanceOf(NullPointerException.class);
  }

  private Map<Integer, Kata22_RaftLeaderElection> cluster(int size) {
    Map<Integer, Kata22_RaftLeaderElection> nodes = new HashMap<>();
    for (int id = 0; id < size; id++) {
      nodes.put(id, newNode(id, size));
    }
    return nodes;
  }

  private Map<Integer, Kata22_RaftLeaderElection> electLeader(int size, int leaderId) {
    Map<Integer, Kata22_RaftLeaderElection> nodes = cluster(size);
    Kata22_RaftLeaderElection leader = nodes.get(leaderId);
    List<Message> requests = leader.onElectionTimeout();
    List<Message> responses = deliver(nodes, requests);
    List<Message> heartbeats = deliver(nodes, responses);
    deliver(nodes, heartbeats);
    return nodes;
  }

  private static List<Message> onlyTo(List<Message> messages, int recipient) {
    return messages.stream().filter(message -> targetOf(message) == recipient).toList();
  }

  private static List<Message> deliver(
      Map<Integer, Kata22_RaftLeaderElection> nodes, List<Message> messages) {
    List<Message> produced = new ArrayList<>();
    for (Message message : messages) {
      Kata22_RaftLeaderElection target = nodes.get(targetOf(message));
      if (target != null) {
        produced.addAll(dispatch(target, message));
      }
    }
    return produced;
  }

  private static int targetOf(Message message) {
    return switch (message) {
      case RequestVoteReq request -> request.to();
      case RequestVoteResp response -> response.to();
      case AppendEntriesReq request -> request.to();
      case AppendEntriesResp response -> response.to();
    };
  }

  private static List<Message> dispatch(Kata22_RaftLeaderElection node, Message message) {
    return switch (message) {
      case RequestVoteReq request -> node.onRequestVote(request);
      case RequestVoteResp response -> node.onRequestVoteResp(response);
      case AppendEntriesReq request -> node.onHeartbeat(request);
      case AppendEntriesResp response -> List.of();
    };
  }
}
