package dev.kata.networking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.networking.Kata25_NioReactorEchoServerSolution;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 25.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata25* -DuseSolution=true</pre>
 */
@DisplayName("Kata 25: однопоточный NIO reactor echo server")
class Kata25_NioReactorEchoServerTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata25_NioReactorEchoServer newServer() {
    return USE_SOLUTION
        ? new Kata25_NioReactorEchoServerSolution()
        : new Kata25_NioReactorEchoServer();
  }

  @Test
  @DisplayName("start/port/close: жизненный цикл и валидация")
  void lifecycle() {
    Kata25_NioReactorEchoServer server = newServer();
    try {
      assertThatThrownBy(server::port).isInstanceOf(IllegalStateException.class);

      server.start();
      server.start();
      int port = server.port();
      assertThat(port).isPositive();
    } finally {
      server.close();
      server.close();
    }

    assertThatThrownBy(server::start).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Одно сообщение: сервер возвращает его с переводом строки")
  void echoSingleMessage() throws Exception {
    try (Kata25_NioReactorEchoServer server = newServer()) {
      server.start();

      try (Socket socket = connect(server.port())) {
        BufferedReader reader = reader(socket);
        write(socket, "hello");
        assertThat(readLine(reader)).isEqualTo("hello");
      }
    }
  }

  @Test
  @DisplayName("Фрагментация TCP: сообщение по одному байту не теряется")
  void fragmentedMessage() throws Exception {
    try (Kata25_NioReactorEchoServer server = newServer()) {
      server.start();

      try (Socket socket = connect(server.port())) {
        BufferedReader reader = reader(socket);
        OutputStream output = socket.getOutputStream();
        byte[] payload = "fragmented-message".getBytes(StandardCharsets.UTF_8);
        for (byte value : payload) {
          output.write(value);
          output.flush();
          Thread.sleep(1);
        }
        output.write('\n');
        output.flush();

        assertThat(readLine(reader)).isEqualTo("fragmented-message");
      }
    }
  }

  @Test
  @DisplayName("Несколько сообщений в одном read() обрабатываются все")
  void multipleMessagesInOneWrite() throws Exception {
    try (Kata25_NioReactorEchoServer server = newServer()) {
      server.start();

      try (Socket socket = connect(server.port())) {
        BufferedReader reader = reader(socket);
        socket.getOutputStream().write("one\ntwo\nthree\n".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();

        assertThat(readLine(reader)).isEqualTo("one");
        assertThat(readLine(reader)).isEqualTo("two");
        assertThat(readLine(reader)).isEqualTo("three");
      }
    }
  }

  @Test
  @DisplayName("Пустые строки и CRLF сохраняются")
  void emptyLinesAndCrlf() throws Exception {
    try (Kata25_NioReactorEchoServer server = newServer()) {
      server.start();

      try (Socket socket = connect(server.port())) {
        BufferedReader reader = reader(socket);
        socket.getOutputStream().write("a\n\nb\r\n".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();

        assertThat(readLine(reader)).isEqualTo("a");
        assertThat(readLine(reader)).isEmpty();
        // Сервер вернул "b\r\n"; BufferedReader.readLine() трактует CRLF как терминатор.
        assertThat(readLine(reader)).isEqualTo("b");
      }
    }
  }

  @Test
  @DisplayName("20 конкурентных клиентов без потерь и перемешивания")
  void twentyConcurrentClients() throws Exception {
    int clientCount = 20;
    int messagesPerClient = 50;

    try (Kata25_NioReactorEchoServer server = newServer()) {
      server.start();
      int port = server.port();

      ExecutorService pool = Executors.newFixedThreadPool(clientCount);
      CountDownLatch ready = new CountDownLatch(clientCount);
      CountDownLatch start = new CountDownLatch(1);
      try {
        List<Future<?>> futures = new ArrayList<>();
        for (int client = 0; client < clientCount; client++) {
          int clientId = client;
          futures.add(
              pool.submit(
                  () -> {
                    ready.countDown();
                    start.await();
                    try (Socket socket = connect(port)) {
                      BufferedReader reader = reader(socket);
                      OutputStream output = socket.getOutputStream();
                      for (int message = 0; message < messagesPerClient; message++) {
                        String text = "client-" + clientId + "-msg-" + message;
                        output.write((text + "\n").getBytes(StandardCharsets.UTF_8));
                        output.flush();
                        if (!text.equals(readLine(reader))) {
                          throw new AssertionError(
                              "unexpected echo for client " + clientId + " message " + message);
                        }
                      }
                    }
                    return null;
                  }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
          future.get(30, TimeUnit.SECONDS);
        }
      } finally {
        pool.shutdownNow();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
      }
    }
  }

  @Test
  @DisplayName("Backpressure: большой ответ записывается через очередь OP_WRITE")
  void largeMessageBackpressure() throws Exception {
    int length = 2_000_000;
    byte[] payload = new byte[length];
    Arrays.fill(payload, (byte) 'x');

    try (Kata25_NioReactorEchoServer server = newServer()) {
      server.start();

      Socket socket = new Socket();
      socket.setReceiveBufferSize(1024);
      socket.setSendBufferSize(1024);
      socket.setSoTimeout(20_000);
      socket.connect(loopback(server.port()), 5_000);
      try (socket) {
        BufferedReader reader = reader(socket);
        OutputStream output = socket.getOutputStream();
        output.write(payload);
        output.write('\n');
        output.flush();

        String echoed = readLine(reader);
        assertThat(echoed).hasSize(length);
        assertThat(echoed.charAt(0)).isEqualTo('x');
        assertThat(echoed.charAt(length - 1)).isEqualTo('x');
      }
    }
  }

  @Test
  @DisplayName("Резкое закрытие клиента не ломает сервер")
  void abruptClientCloseDoesNotKillServer() throws Exception {
    try (Kata25_NioReactorEchoServer server = newServer()) {
      server.start();

      Socket abrupt = connect(server.port());
      abrupt.close();

      try (Socket socket = connect(server.port())) {
        BufferedReader reader = reader(socket);
        write(socket, "still-alive");
        assertThat(readLine(reader)).isEqualTo("still-alive");
      }
    }
  }

  @Test
  @DisplayName("close закрывает клиентов, освобождает порт и идемпотентен")
  void closeReleasesResources() throws Exception {
    Kata25_NioReactorEchoServer server = newServer();
    server.start();
    int port = server.port();

    Socket socket = connect(port);
    try {
      BufferedReader reader = reader(socket);
      write(socket, "bye");
      assertThat(readLine(reader)).isEqualTo("bye");

      server.close();
      server.close();

      boolean closed;
      try {
        closed = reader.read() == -1;
      } catch (IOException expectedReset) {
        closed = true;
      }
      assertThat(closed).isTrue();
    } finally {
      socket.close();
    }

    assertThatThrownBy(() -> connect(port)).isInstanceOf(IOException.class);
  }

  private static Socket connect(int port) throws IOException {
    Socket socket = new Socket();
    socket.setSoTimeout(10_000);
    socket.connect(loopback(port), 5_000);
    return socket;
  }

  private static InetSocketAddress loopback(int port) {
    return new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
  }

  private static void write(Socket socket, String text) throws IOException {
    socket.getOutputStream().write((text + "\n").getBytes(StandardCharsets.UTF_8));
    socket.getOutputStream().flush();
  }

  private static BufferedReader reader(Socket socket) throws IOException {
    return new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
  }

  private static String readLine(BufferedReader reader) throws IOException {
    return reader.readLine();
  }
}
