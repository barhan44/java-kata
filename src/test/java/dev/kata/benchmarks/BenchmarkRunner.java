package dev.kata.benchmarks;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Точка входа для JMH без сборки uber-jar.
 *
 * <h2>Как запускать</h2>
 *
 * <pre>
 * # все бенчмарки против эталонных решений
 * mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
 *     -Dexec.args="" -DuseSolution=true
 *
 * # только ring buffer
 * mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
 *     -Dexec.args="RingBuffer" -DuseSolution=true
 * </pre>
 *
 * <p>В IDE достаточно запустить {@code main} этого класса как обычное Java-приложение:
 * exec-maven-plugin не нужен, JMH сам создаст форк JVM и подхватит classpath из IDE.
 *
 * <h2>Почему GCProfiler</h2>
 *
 * <p>{@link GCProfiler} добавляет к результатам колонки {@code gc.alloc.rate} и {@code
 * gc.alloc.rate.norm} — аллокации в байтах на одну операцию. Для katas про память и lock-free
 * структуры это важнее, чем «голый» throughput: можно увидеть, что реализация быстрая, но
 * аллоцирует 48 B/op, и на длинной дистанции это вызовет GC-паузы.
 *
 * <h2>Переключатель useSolution</h2>
 *
 * <p>Бенчмарки, как и тесты, по умолчанию запускают заготовки. Свойство {@code useSolution}
 * пробрасывается в форк JVM явно через {@code jvmArgsAppend}: JMH не копирует произвольные {@code
 * -D}-свойства родительского процесса в дочерний, поэтому без этого флага форк не увидел бы
 * настройку.
 *
 * <h2>Аргументы</h2>
 *
 * <p>Любые аргументы командной строки трактуются как подстроки имён бенчмарков и объединяются в
 * regex: {@code "RingBuffer RateLimiter"} запустит оба набора. Без аргументов запускаются все
 * классы, оканчивающиеся на {@code Benchmark}.
 */
public final class BenchmarkRunner {

  private BenchmarkRunner() {}

  public static void main(String[] args) throws RunnerException {
    ensureForkedClasspath();
    String include = buildIncludeRegex(args);
    boolean useSolution = Boolean.getBoolean("useSolution");

    ChainedOptionsBuilder options =
        new OptionsBuilder()
            .include(include)
            .addProfiler(GCProfiler.class)
            .jvmArgsAppend("-DuseSolution=" + useSolution, "--enable-preview")
            .shouldFailOnError(true);

    new Runner(options.build()).run();
  }

  /**
   * Чинит classpath форка при запуске через {@code exec:java}.
   *
   * <p>JMH строит команду для дочерней JVM из {@code System.getProperty("java.class.path")}. {@code
   * exec:java} же запускает код в текущей JVM через {@code URLClassLoader} Maven и не обновляет это
   * свойство — в нём остаётся только загрузчик Maven. В результате форк падает с {@code
   * ClassNotFoundException: org.openjdk.jmh.runner.ForkedMain}. Поэтому собираем пути из URL-ов
   * контекстного загрузчика (ClassRealm — наследник {@link URLClassLoader}) и дописываем их в
   * свойство.
   *
   * <p>При запуске из IDE {@code java.class.path} уже корректен, и метод ничего не меняет.
   */
  private static void ensureForkedClasspath() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (!(loader instanceof URLClassLoader urlLoader)) {
      return;
    }

    Set<String> entries = new LinkedHashSet<>();
    String existing = System.getProperty("java.class.path", "");
    if (!existing.isEmpty()) {
      entries.addAll(Arrays.asList(existing.split(File.pathSeparator)));
    }
    for (URL url : urlLoader.getURLs()) {
      if (!"file".equals(url.getProtocol())) {
        continue;
      }
      try {
        entries.add(Paths.get(url.toURI()).toString());
      } catch (URISyntaxException ignored) {
        // Некорректный URL просто пропускаем — он не нужен форку.
      }
    }
    System.setProperty("java.class.path", String.join(File.pathSeparator, entries));
  }

  /** Собирает regex для {@link org.openjdk.jmh.runner.options.Options#getIncludes()}. */
  static String buildIncludeRegex(String[] args) {
    if (args.length == 0) {
      return ".*Benchmark";
    }
    return ".*(" + String.join("|", args) + ").*";
  }
}
