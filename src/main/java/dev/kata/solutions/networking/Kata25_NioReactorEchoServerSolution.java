package dev.kata.solutions.networking;

import dev.kata.networking.Kata25_NioReactorEchoServer;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Эталонное решение Kata 25.
 *
 * <h2>Event loop</h2>
 *
 * <p>{@link #start()} создаёт неблокирующий {@link ServerSocketChannel}, регистрирует его на {@code
 * OP_ACCEPT} и запускает один поток. Цикл:
 *
 * <pre>
 *   selector.select()
 *   for each ready key:
 *     accept -> register new SocketChannel with OP_READ
 *     read   -> accumulate bytes, emit complete '\n'-terminated lines
 *     write  -> drain the per-connection ByteBuffer queue
 * </pre>
 *
 * <p>Все операции с каналами выполняются в одном потоке. Поэтому состояние соединения ({@code
 * readBuffer}, аккумулятор строки, очередь записи) не требует блокировок. Поток daemon: забытый
 * {@code close()} не помешает JVM завершиться, но тесты и production-код обязаны закрывать сервер
 * явно.
 *
 * <h2>Framing</h2>
 *
 * <p>TCP — поток байтов, а не сообщений. Один {@code read()} может вернуть половину строки,
 * несколько строк или один байт. Аккумулятор {@link ByteArrayOutputStream} копит байты между
 * вызовами; при встрече {@code '\n'} строка копируется в исходящий буфер и сбрасывается. Сервер
 * возвращает payload вместе с {@code '\n'}; клиентский {@code readLine()} получает ровно
 * отправленную строку.
 *
 * <h2>Backpressure и OP_WRITE</h2>
 *
 * <p>После чтения сервер сразу пытается записать эхо. {@code SocketChannel.write()} неблокирующий и
 * может вернуть 0 или записать только часть. Остаток остаётся в {@link ArrayDeque}; канал
 * подписывается на {@code OP_WRITE}. Когда selector сообщает о готовности, {@code flush} продолжает
 * с того места, где остановился, и снимает {@code OP_WRITE}, как только очередь пуста. Постоянная
 * подписка на {@code OP_WRITE} была бы ошибкой: этот флаг почти всегда готов и приводит к
 * busy-loop.
 *
 * <h2>Ошибки и закрытие</h2>
 *
 * <p>Исключение чтения/записи закрывает только проблемное соединение; остальные ключи продолжают
 * обрабатываться. {@link #close()} выставляет {@code running=false}, будит selector и join-ит
 * поток; {@code finally} event loop закрывает все клиентские каналы, серверный канал и selector.
 * Операция идемпотентна.
 *
 * <h2>Аллокации</h2>
 *
 * <p>На соединение — один 4 KiB read-buffer и аккумулятор. На каждую строку — копия payload и
 * {@link ByteBuffer} для эха. Это осознанный компромисс: API должен отдать клиенту самостоятельный
 * буфер, а не ссылку на переиспользуемый read-buffer. Для zero-copy можно использовать {@code
 * GatheringByteBuffer} или пул буферов, но это усложнило бы пример.
 */
public final class Kata25_NioReactorEchoServerSolution extends Kata25_NioReactorEchoServer {

  private static final int READ_BUFFER_SIZE = 4096;
  private static final byte NEWLINE = '\n';

  private final Object lifecycleLock = new Object();
  private final Set<ClientConnection> clients = new HashSet<>();

  private Selector selector;
  private ServerSocketChannel serverChannel;
  private Thread eventLoopThread;
  private volatile boolean running;
  private volatile int port;
  private boolean started;
  private boolean closed;

  @Override
  public void start() {
    synchronized (lifecycleLock) {
      if (closed) {
        throw new IllegalStateException("server is closed");
      }
      if (started) {
        return;
      }

      Selector newSelector = null;
      ServerSocketChannel newServer = null;
      Thread newThread = null;
      try {
        newSelector = Selector.open();
        newServer = ServerSocketChannel.open();
        newServer.configureBlocking(false);
        newServer.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 128);
        newServer.register(newSelector, SelectionKey.OP_ACCEPT);

        newThread =
            Thread.ofPlatform().daemon().name("kata25-nio-reactor").unstarted(this::runLoop);

        this.selector = newSelector;
        this.serverChannel = newServer;
        this.eventLoopThread = newThread;
        this.port = ((InetSocketAddress) newServer.getLocalAddress()).getPort();
        this.running = true;
        this.started = true;
        newThread.start();
      } catch (IOException e) {
        closeQuietly(newServer);
        closeQuietly(newSelector);
        resetAfterFailedStart();
        throw new UncheckedIOException("cannot start NIO echo server", e);
      } catch (RuntimeException e) {
        closeQuietly(newServer);
        closeQuietly(newSelector);
        resetAfterFailedStart();
        throw e;
      }
    }
  }

  @Override
  public int port() {
    synchronized (lifecycleLock) {
      if (!started) {
        throw new IllegalStateException("server is not started");
      }
      return port;
    }
  }

  @Override
  public void close() {
    Thread toJoin;
    synchronized (lifecycleLock) {
      if (closed) {
        return;
      }
      closed = true;
      running = false;
      if (selector != null) {
        selector.wakeup();
      }
      toJoin = eventLoopThread;
    }

    if (toJoin != null && toJoin != Thread.currentThread()) {
      try {
        toJoin.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void runLoop() {
    try {
      while (running) {
        selector.select();
        if (!running) {
          break;
        }

        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          iterator.remove();
          if (!key.isValid()) {
            continue;
          }

          try {
            if (key.channel() == serverChannel) {
              acceptConnections();
            } else {
              ClientConnection connection = (ClientConnection) key.attachment();
              if (connection == null) {
                continue;
              }
              if (key.isReadable()) {
                readFrom(connection, key);
              }
              if (key.isValid() && key.isWritable()) {
                writeTo(connection);
              }
            }
          } catch (CancelledKeyException ignored) {
            // Ключ уже отменён при закрытии соединения.
          } catch (IOException e) {
            if (key.channel() == serverChannel) {
              running = false;
              break;
            }
            closeClient(key);
          }
        }
      }
    } catch (IOException ignored) {
      // Селектор сломался/закрыт: завершаем цикл и освобождаем ресурсы в finally.
    } finally {
      running = false;
      shutdown();
    }
  }

  private void acceptConnections() throws IOException {
    while (running) {
      SocketChannel channel = serverChannel.accept();
      if (channel == null) {
        return;
      }

      try {
        channel.configureBlocking(false);
      } catch (IOException e) {
        closeQuietly(channel);
        throw e;
      }

      ClientConnection connection = new ClientConnection(channel);
      clients.add(connection);
      try {
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ, connection);
        connection.key = key;
      } catch (IOException | RuntimeException e) {
        clients.remove(connection);
        closeQuietly(channel);
        throw e;
      }
    }
  }

  private void readFrom(ClientConnection connection, SelectionKey key) throws IOException {
    ByteBuffer buffer = connection.readBuffer;
    buffer.clear();
    int read = connection.channel.read(buffer);
    if (read == -1) {
      closeClient(key);
      return;
    }
    if (read == 0) {
      return;
    }

    buffer.flip();
    while (buffer.hasRemaining()) {
      byte value = buffer.get();
      if (value == NEWLINE) {
        byte[] payload = connection.line.toByteArray();
        connection.line.reset();
        enqueue(connection, payload);
      } else {
        connection.line.write(value & 0xFF);
      }
    }
  }

  private void writeTo(ClientConnection connection) throws IOException {
    flush(connection);
  }

  private void enqueue(ClientConnection connection, byte[] payload) throws IOException {
    ByteBuffer output = ByteBuffer.allocate(payload.length + 1);
    output.put(payload).put(NEWLINE).flip();
    connection.pendingWrites.addLast(output);
    flush(connection);
  }

  private void flush(ClientConnection connection) throws IOException {
    while (!connection.pendingWrites.isEmpty()) {
      ByteBuffer buffer = connection.pendingWrites.peekFirst();
      int written = connection.channel.write(buffer);
      if (written == 0) {
        break;
      }
      if (buffer.hasRemaining()) {
        break;
      }
      connection.pendingWrites.removeFirst();
    }

    SelectionKey key = connection.key;
    if (key == null || !key.isValid()) {
      return;
    }
    int interest = key.interestOps();
    if (connection.pendingWrites.isEmpty()) {
      interest &= ~SelectionKey.OP_WRITE;
    } else {
      interest |= SelectionKey.OP_WRITE;
    }
    key.interestOps(interest);
  }

  private void closeClient(SelectionKey key) {
    if (key == null) {
      return;
    }
    if (key.channel() == serverChannel) {
      running = false;
      return;
    }
    if (key.attachment() instanceof ClientConnection connection) {
      clients.remove(connection);
    }
    key.cancel();
    closeQuietly(key.channel());
  }

  private void shutdown() {
    for (ClientConnection connection : clients) {
      closeQuietly(connection.channel);
    }
    clients.clear();
    closeQuietly(serverChannel);
    closeQuietly(selector);
  }

  private void resetAfterFailedStart() {
    selector = null;
    serverChannel = null;
    eventLoopThread = null;
    running = false;
    started = false;
  }

  private static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ignored) {
        // Best effort.
      }
    }
  }

  /** Состояние одного клиента; доступно только event loop потоку. */
  private static final class ClientConnection {
    final SocketChannel channel;
    final ByteBuffer readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
    final ByteArrayOutputStream line = new ByteArrayOutputStream();
    final ArrayDeque<ByteBuffer> pendingWrites = new ArrayDeque<>();
    SelectionKey key;

    ClientConnection(SocketChannel channel) {
      this.channel = channel;
    }
  }
}
