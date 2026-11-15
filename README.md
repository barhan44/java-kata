# java-kata

Ежедневные упражнения для Senior Java-разработчика: concurrency, memory, algorithms,
streams, internals, распределённые системы и продвинутая доменная логика. В проекте
**31 kata** в пяти волнах, включая блок **Prod-Ready Craft** (Collections, Streams &
Domain Logic).

Каждая kata — это **три файла**:

1. **Заготовка** с подробным Javadoc, контрактом и методами, которые бросают
   `UnsupportedOperationException("Not implemented yet")`.
2. **Эталонное решение** с идиоматичной реализацией и разбором компромиссов.
3. **Тесты** на JUnit 5 + AssertJ + Awaitility, покрывающие базовые сценарии, edge-cases и
   многопоточную нагрузку.

По умолчанию `mvn test` запускает тесты **против заготовок** — они падают, и это
задумано. С флагом `-DuseSolution=true` те же тесты запускаются против эталона и должны
быть зелёными.

---

## Требования

| Инструмент | Версия |
|---|---|
| JDK | **21** (компиляция с `--enable-preview`) |
| Maven | 3.9+ |

> **Важно:** `mvn -version` должен показывать Java 21. Скомпилированные с
> `--enable-preview` классы нельзя загрузить на другой версии JDK. Если Maven запускается
> на JDK 27, укажите `JAVA_HOME` явно:
>
> ```bash
> export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS
> # export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk     # Linux
> ```

---

## Структура проекта

```text
java-kata/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/java/dev/kata/
    │   ├── concurrency/
    │   │   ├── Kata01_StripedLockCache.java        # заготовка
    │   │   ├── Kata02_AsyncTaskBatcher.java
    │   │   ├── Kata08_StructuredScatterGather.java
    │   │   ├── Kata09_KeyedSequentialExecutor.java
    │   │   └── Kata10_TreiberStack.java
    │   ├── patterns/
    │   │   ├── Kata03_TokenBucketRateLimiter.java
    │   │   ├── Kata11_CircuitBreaker.java
    │   │   └── Kata12_ConsistentHashingRouter.java
    │   ├── algorithms/
    │   │   ├── Kata04_RingBuffer.java
    │   │   ├── Kata13_BloomFilter.java
    │   │   ├── Kata16_HashedWheelTimer.java
    │   │   └── Kata17_RadixUrlRouter.java
    │   ├── streams/
    │   │   ├── Kata05_CustomSpliterator.java
    │   │   └── Kata18_CustomStreamGatherer.java
    │   ├── memory/
    │   │   ├── Kata06_OffHeapByteBufferPool.java
    │   │   ├── Kata14_ArenaSlabAllocator.java
    │   │   └── Kata15_ZeroCopyCsvParser.java
    │   ├── internals/
    │   │   ├── Kata07_MiniDependencyInjector.java
    │   │   └── Kata19_MethodHandleInterceptor.java
    │   ├── storage/
    │   │   ├── Kata20_WriteAheadLog.java
    │   │   └── Kata21_LsmMemTableAndSstable.java
    │   ├── distributed/
    │   │   ├── Kata22_RaftLeaderElection.java
    │   │   ├── Kata23_HybridLogicalClock.java
    │   │   └── Kata24_OrSetCrdt.java
    │   ├── networking/
    │   │   └── Kata25_NioReactorEchoServer.java
    │   ├── prod/
    │   │   ├── Kata26_LedgerReconciliation.java
    │   │   ├── Kata27_IntervalScheduleMerger.java
    │   │   ├── Kata28_StreamAnalyticsPipeline.java
    │   │   ├── Kata29_HierarchicalTreeBuilder.java
    │   │   ├── Kata30_PermissionBitmaskEvaluator.java
    │   │   └── Kata31_ResilientDataMapper.java
    │   └── solutions/                              # эталоны, повторяют структуру пакетов
    │       ├── concurrency/Kata01_StripedLockCacheSolution.java
    │       ├── concurrency/Kata02_AsyncTaskBatcherSolution.java
    │       ├── concurrency/Kata08_StructuredScatterGatherSolution.java
    │       ├── concurrency/Kata09_KeyedSequentialExecutorSolution.java
    │       ├── concurrency/Kata10_TreiberStackSolution.java
    │       ├── patterns/Kata03_TokenBucketRateLimiterSolution.java
    │       ├── patterns/Kata11_CircuitBreakerSolution.java
    │       ├── patterns/Kata12_ConsistentHashingRouterSolution.java
    │       ├── algorithms/Kata04_RingBufferSolution.java
    │       ├── algorithms/Kata13_BloomFilterSolution.java
    │       ├── algorithms/Kata16_HashedWheelTimerSolution.java
    │       ├── algorithms/Kata17_RadixUrlRouterSolution.java
    │       ├── streams/Kata05_CustomSpliteratorSolution.java
    │       ├── streams/Kata18_CustomStreamGathererSolution.java
    │       ├── memory/Kata06_OffHeapByteBufferPoolSolution.java
    │       ├── memory/Kata14_ArenaSlabAllocatorSolution.java
    │       ├── memory/Kata15_ZeroCopyCsvParserSolution.java
    │       ├── internals/Kata07_MiniDependencyInjectorSolution.java
    │       ├── internals/Kata19_MethodHandleInterceptorSolution.java
    │       ├── storage/Kata20_WriteAheadLogSolution.java
    │       ├── storage/Kata21_LsmMemTableAndSstableSolution.java
    │       ├── distributed/Kata22_RaftLeaderElectionSolution.java
    │       ├── distributed/Kata23_HybridLogicalClockSolution.java
    │       ├── distributed/Kata24_OrSetCrdtSolution.java
    │       ├── networking/Kata25_NioReactorEchoServerSolution.java
    │       ├── prod/Kata26_LedgerReconciliationSolution.java
    │       ├── prod/Kata27_IntervalScheduleMergerSolution.java
    │       ├── prod/Kata28_StreamAnalyticsPipelineSolution.java
    │       ├── prod/Kata29_HierarchicalTreeBuilderSolution.java
    │       ├── prod/Kata30_PermissionBitmaskEvaluatorSolution.java
    │       └── prod/Kata31_ResilientDataMapperSolution.java
    └── test/
        ├── java/dev/kata/
        │   ├── concurrency/Kata01_StripedLockCacheTest.java
        │   ├── concurrency/Kata02_AsyncTaskBatcherTest.java
        │   ├── concurrency/Kata08_StructuredScatterGatherTest.java
        │   ├── concurrency/Kata09_KeyedSequentialExecutorTest.java
        │   ├── concurrency/Kata10_TreiberStackTest.java
        │   ├── patterns/Kata03_TokenBucketRateLimiterTest.java
        │   ├── patterns/Kata11_CircuitBreakerTest.java
        │   ├── patterns/Kata12_ConsistentHashingRouterTest.java
        │   ├── algorithms/Kata04_RingBufferTest.java
        │   ├── algorithms/Kata13_BloomFilterTest.java
        │   ├── algorithms/Kata16_HashedWheelTimerTest.java
        │   ├── algorithms/Kata17_RadixUrlRouterTest.java
        │   ├── streams/Kata05_CustomSpliteratorTest.java
        │   ├── streams/Kata18_CustomStreamGathererTest.java
        │   ├── memory/Kata06_OffHeapByteBufferPoolTest.java
        │   ├── memory/Kata14_ArenaSlabAllocatorTest.java
        │   ├── memory/Kata15_ZeroCopyCsvParserTest.java
        │   ├── internals/Kata07_MiniDependencyInjectorTest.java
        │   ├── internals/Kata19_MethodHandleInterceptorTest.java
        │   ├── storage/Kata20_WriteAheadLogTest.java
        │   ├── storage/Kata21_LsmMemTableAndSstableTest.java
        │   ├── distributed/Kata22_RaftLeaderElectionTest.java
        │   ├── distributed/Kata23_HybridLogicalClockTest.java
        │   ├── distributed/Kata24_OrSetCrdtTest.java
        │   ├── networking/Kata25_NioReactorEchoServerTest.java
        │   ├── prod/Kata26_LedgerReconciliationTest.java
        │   ├── prod/Kata27_IntervalScheduleMergerTest.java
        │   ├── prod/Kata28_StreamAnalyticsPipelineTest.java
        │   ├── prod/Kata29_HierarchicalTreeBuilderTest.java
        │   ├── prod/Kata30_PermissionBitmaskEvaluatorTest.java
        │   ├── prod/Kata31_ResilientDataMapperTest.java
        │   └── benchmarks/
        │       ├── BenchmarkRunner.java
        │       ├── ConsistentHashingBenchmark.java
        │       ├── RateLimiterBenchmark.java
        │       ├── SlabAllocatorBenchmark.java
        │       ├── RingBufferBenchmark.java
        │       ├── StripedCacheBenchmark.java
        │       └── WalAndHlcBenchmark.java
        └── resources/logback-test.xml
```

Эталонные решения **наследуются от заготовок** и переопределяют методы. Поэтому тест
работает с одним типом `KataXX_<Name>`, а фабрика в тесте выбирает реализацию:

```java
private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

private <K, V> Kata01_StripedLockCache<K, V> newCache(int stripes, long ttlMillis) {
    return USE_SOLUTION
            ? new Kata01_StripedLockCacheSolution<>(stripes, ttlMillis)
            : new Kata01_StripedLockCache<>(stripes, ttlMillis);
}
```

---

## Шпаргалка по Maven-командам

> В zsh/bash звёздочку в `-Dtest=...` нужно брать в кавычки, иначе shell раскроет её как
> glob и Maven получит список файлов.

### Тесты

```bash
# Все тесты против заготовок — ожидаемо падают с UnsupportedOperationException
mvn test

# Все тесты против эталонных решений — должны быть зелёными
mvn test -DuseSolution=true

# Одна kata против заготовки
mvn test -Dtest='Kata01*'

# Одна kata против эталона
mvn test -Dtest='Kata01*' -DuseSolution=true

# Несколько katas
mvn test -Dtest='Kata0[1-3]*' -DuseSolution=true

# Один конкретный тест-метод
mvn test -Dtest='Kata01_StripedLockCacheTest#stampedeProtection' -DuseSolution=true
```

Surefire настроен на вывод статусов в консоль (`redirectTestOutputToFile=false`), поэтому
видны имена тестов (`@DisplayName` на русском), количество запусков и стектрейсы.

### Эталон и сброс задачи

```bash
# Посмотреть эталон
cat src/main/java/dev/kata/concurrency/Kata01_StripedLockCacheSolution.java

# Сбросить свою реализацию заготовки к исходному состоянию
git checkout -- src/main/java/dev/kata/concurrency/Kata01_StripedLockCache.java

# Сбросить сразу все заготовки
git checkout -- src/main/java/dev/kata/
```

### Форматирование и покрытие

```bash
# Отформатировать код Google Java Format
mvn spotless:apply

# Только проверить форматирование (падает при нарушениях)
mvn spotless:check

# Полная проверка: тесты + spotless:check + отчёт JaCoCo
mvn verify -DuseSolution=true

# HTML-отчёт покрытия
open target/site/jacoco/index.html
```

`spotless:check` привязан к фазе `verify`; `mvn test` его не запускает. JaCoCo-агент
включается в `initialize`, отчёт генерируется на `verify`.

---

## JMH-бенчмарки

Бенчмарки лежат в `src/test/java` и запускаются через `BenchmarkRunner` — **uber-jar не
нужен**. Профиль учебный: `@Warmup(iterations = 2, time = 1)`,
`@Measurement(iterations = 3, time = 1)`, `@Fork(1)`.

```bash
# Все бенчмарки против эталонов
mvn test-compile exec:java \
  -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
  -Dexec.args="" -DuseSolution=true

# Только ring buffer (фильтр — подстрока имени)
mvn test-compile exec:java \
  -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
  -Dexec.args="RingBuffer" -DuseSolution=true

# Rate limiter и striped cache
mvn test-compile exec:java \
  -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
  -Dexec.args="RateLimiter StripedCache" -DuseSolution=true

# Маршрутизация консистентного хеширования (Kata 12)
mvn test-compile exec:java \
  -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
  -Dexec.args="ConsistentHashing" -DuseSolution=true

# Off-heap slab allocator vs heap-массивы (Kata 14)
mvn test-compile exec:java \
  -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
  -Dexec.args="SlabAllocator" -DuseSolution=true

# HLC zero-allocation и WAL append (Kata 20/23)
mvn test-compile exec:java \
  -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" \
  -Dexec.args="WalAndHlc" -DuseSolution=true
```

В IDE достаточно запустить `BenchmarkRunner#main` как обычное Java-приложение с VM-опцией
`-DuseSolution=true`. `BenchmarkRunner` сам чинит classpath для `exec:java` (JMH строит
команду форка из `java.class.path`, который Maven не обновляет) и пробрасывает
`useSolution` в дочернюю JVM.

### Что измеряют бенчмарки

| Бенчмарк | Сценарий | Режимы |
|---|---|---|
| `RateLimiterBenchmark` | Token Bucket в 1 и 8 потоках | `Throughput`, `AverageTime` |
| `RingBufferBenchmark` | SPSC ring buffer vs `ArrayBlockingQueue`, producer/consumer groups | `Throughput` |
| `StripedCacheBenchmark` | lock-striped кэш vs `Collections.synchronizedMap`, 90% чтений / 10% записей, 8 потоков | `Throughput`, `AverageTime` |
| `ConsistentHashingBenchmark` | `route(key)` по кольцу (5 нод × 200 vnodes) vs modulo-baseline, 8 потоков, проверка нулевых аллокаций | `Throughput`, `AverageTime` |
| `SlabAllocatorBenchmark` | off-heap `allocate/free` vs `new byte[64]` / `new long[8]`; `gc.alloc.rate.norm ≈ 0` у slab | `Throughput`, `AverageTime` |
| `WalAndHlcBenchmark` | HLC `nowPacked`/`updatePacked` (0 B/op) и WAL `append` с `force(false)` | `Throughput`, `AverageTime` |

`BenchmarkRunner` подключает `GCProfiler`, поэтому в результатах есть
`gc.alloc.rate.norm` — **байты на операцию**. Это ключевая метрика для katas про память:
быстрая реализация, аллоцирующая 32 B/op, на длинной дистанции проиграет из-за GC.

### Памятка по аннотациям JMH

| Аннотация | Смысл |
|---|---|
| `@Benchmark` | Метод, который JMH измеряет. Возвращаемое значение JMH потребляет (blackhole). |
| `@BenchmarkMode` | `Throughput` (ops/time), `AverageTime` (time/op), `SampleTime`, `SingleShotTime`, `All`. |
| `@OutputTimeUnit` | Единица вывода: `NANOSECONDS`, `MICROSECONDS`, `MILLISECONDS`, `SECONDS`. |
| `@Warmup(iterations, time)` | Прогрев до измерений: JIT-компиляция и стабилизация кэшей. |
| `@Measurement(iterations, time)` | Собственно измерения; JMH показывает разброс и доверительный интервал. |
| `@Fork(n)` | Сколько отдельных JVM запустить. Форк изолирует состояние и профилировщики. |
| `@Threads(n)` | Число потоков. Можно ставить на класс или на метод. |
| `@State(Scope)` | Объект состояния: `Benchmark` (общий), `Thread` (свой на поток), `Group` (общий на группу). |
| `@Setup` / `@TearDown` | Подготовка/очистка на уровне `Trial`, `Iteration` или `Invocation`. |
| `@Param` | Параметризация бенчмарка значениями; JMH прогонит все комбинации. |
| `@Group` / `@GroupThreads` | Конкурентная «сцена» из нескольких методов; нужна для producer/consumer. |
| `Blackhole` | Потребляет результат, чтобы JIT не выбросил полезную работу. |
| `@CompilerControl` | Управление инлайнингом (`DONT_INLINE`, `INLINE`) для проверки гипотез. |
| `-prof gc` / `GCProfiler` | Аллокации и время GC в байтах на операцию. |

---

## Список задач

В проекте **31 задача** (волны 1–5).

| № | Kata | Тема | Ключевые навыки |
|---|---|---|---|
| 01 | `Kata01_StripedLockCache` | Потокобезопасный кэш | Lock striping, `ReadWriteLock`, TTL на `nanoTime`, lazy expiration, cache stampede, `LongAdder` |
| 02 | `Kata02_AsyncTaskBatcher` | Micro-batching | `CompletableFuture`, `ScheduledExecutorService`, гонки таймера и размера пачки, корректная обработка ошибок |
| 03 | `Kata03_TokenBucketRateLimiter` | Rate limiting | Lock-free CAS, `AtomicReference`, token bucket, точность `nanoTime`, отсутствие записи при отказе |
| 04 | `Kata04_RingBuffer` | SPSC ring buffer | `volatile`-индексы, happens-before, padding против false sharing, zero-allocation hot path |
| 05 | `Kata05_CustomSpliterator` | Ленивые стримы | `Spliterator`, `trySplit`, `parallelStream`, ленивый парсинг, характеристики |
| 06 | `Kata06_OffHeapByteBufferPool` | Off-heap память | `ByteBuffer.allocateDirect`, пул, `AutoCloseable`, `Cleaner`, защита от утечек и двойного возврата |
| 07 | `Kata07_MiniDependencyInjector` | DI-контейнер | Reflection, `MethodHandles`, граф зависимостей, циклы, singleton/prototype, потокобезопасность |
| 08 | `Kata08_StructuredScatterGather` | Hedged requests | Виртуальные потоки, speculative retry, отмена/`interrupt`, агрегация ошибок, CAS на победителя |
| 09 | `Kata09_KeyedSequentialExecutor` | Actor / partition consumer | FIFO по ключу, параллелизм разных ключей, `ConcurrentHashMap.compute`, безопасное удаление очередей, отсутствие утечек |
| 10 | `Kata10_TreiberStack` | Lock-free стек | CAS-цикл, `AtomicStampedReference`, ABA-проблема, `LongAdder`, happens-before |
| 11 | `Kata11_CircuitBreaker` | Отказоустойчивость | Кольцевое окно, три состояния, HALF_OPEN-гейт, инъекция часов, конкурентные переходы |
| 12 | `Kata12_ConsistentHashingRouter` | Шардирование | Кольцо и виртуальные ноды, FNV-1a/MurmurHash3, copy-on-write снимок, минимизация миграции, zero-allocation hot path |
| 13 | `Kata13_BloomFilter` | Вероятностные структуры | Расчёт `m`/`k`, Kirsch–Mitzenmacher, битовая упаковка в `long[]`, контроль false positive rate |
| 14 | `Kata14_ArenaSlabAllocator` | Off-heap slab allocator | `Arena.ofShared`, `MemorySegment`, intrusive free-list, zero-GC, double-free protection |
| 15 | `Kata15_ZeroCopyCsvParser` | Zero-copy парсинг | `MemorySegment`, callback-срезы, кавычки/`""`/CRLF, `parseLong` без `String` |
| 16 | `Kata16_HashedWheelTimer` | Планировщик таймеров | Hashed wheel, O(1) schedule/cancel, `remainingRounds`, догоняющий тикер |
| 17 | `Kata17_RadixUrlRouter` | HTTP-роутинг | Radix Tree, splitting узлов, `{param}`/`*wildcard`, приоритеты и backtracking |
| 18 | `Kata18_CustomStreamGatherer` | Stream API internals | Собственный `StreamGatherer`, `Spliterators.AbstractSpliterator`, ленивость, окна/батчи |
| 19 | `Kata19_MethodHandleInterceptor` | AOP-прокси | `Proxy`, `MethodHandle`, `privateLookupIn`, `invokeDefault`, кэш и цепочка интерсепторов |
| 20 | `Kata20_WriteAheadLog` | Write-Ahead Log | `FileChannel`, CRC32, torn writes, recovery, prefix compaction, монотонные LSN |
| 21 | `Kata21_LsmMemTableAndSstable` | LSM-Tree | MemTable, Tombstone, SSTable, Sparse Index, flush, multi-level lookup |
| 22 | `Kata22_RaftLeaderElection` | Консенсус Raft | Роли, термы, votedFor, majority, Log Up-to-Date, split-vote, step-down |
| 23 | `Kata23_HybridLogicalClock` | Распределённое время | HLC, логический счётчик, clock skew, 48/16-битная упаковка, zero-allocation |
| 24 | `Kata24_OrSetCrdt` | CRDT | OR-Set, уникальные теги, Add-Wins, коммутативность/ассоциативность/идемпотентность |
| 25 | `Kata25_NioReactorEchoServer` | Сетевой event loop | `Selector`, non-blocking I/O, framing, `OP_WRITE` backpressure, graceful close |
| 26 | `Kata26_LedgerReconciliation` | Сверка платежей | `BigDecimal.compareTo`, валюты, `maxTimeDrift`, grace period, четыре категории расхождений за O(N+M) |
| 27 | `Kata27_IntervalScheduleMerger` | Расписание занятости | `TreeMap<Instant, Instant>`, floor/ceiling, слияние/разрезание интервалов, поиск свободных окон |
| 28 | `Kata28_StreamAnalyticsPipeline` | Аналитика заказов | `Collectors.teeing`, `groupingBy`/`flatMapping`/`filtering`, один проход, топ-3 без ничьих-неоднозначностей |
| 29 | `Kata29_HierarchicalTreeBuilder` | Дерево из плоского списка | Итеративное построение леса, сортировка уровней, сироты, циклы с путём, глубина 100 000 без переполнения стека |
| 30 | `Kata30_PermissionBitmaskEvaluator` | RBAC + ABAC | `EnumSet` как битовая маска, наследование ролей, DENY wins, условные правила, volatile-снимок без lock на чтении |
| 31 | `Kata31_ResilientDataMapper` | Валидация импорта | Applicative `combine` с накоплением ошибок, короткозамыкающий `flatMap`, нормализация грязных данных |

### Prod-Ready Craft (Collections, Streams & Domain Logic)

Волна 5 (**Kata26–Kata31**) — это прикладная логика, максимально близкая к продакшену.
Здесь нет экзотических структур: только `java.util`, Stream API, `java.time`, records и
pattern matching из JDK 21. Ценность — в корректной семантике:

- **Kata26** сравнивает деньги через `BigDecimal.compareTo` (а не `equals`), учитывает
  `maxTimeDrift` и не объявляет свежие операции пропавшими раньше grace period.
- **Kata27** держит занятость в `TreeMap` и поддерживает инвариант «отсортировано,
  не пересекается, не смежно», поэтому `book`/`cancel` работают через `floorEntry`/
  `ceilingEntry`, а не перебором.
- **Kata28** собирает три независимых агрегата за один терминальный `collect` с
  вложенным `Collectors.teeing`; ничьи в топе разрешаются по `productId`, пустой поток
  не даёт `NaN`.
- **Kata29** строит лес из плоского списка итеративно: дубликаты id, сироты и циклы
  диагностируются с понятными исключениями, а не `StackOverflowError`.
- **Kata30** моделирует права как `EnumSet`-битмаску, наследует роли с защитой от циклов
  и публикует неизменяемый снимок через `volatile`; `DENY` побеждает всегда.
- **Kata31** разделяет applicative-проверки (все ошибки сразу) и monadic-зависимости
  (`flatMap` короткозамыкает), возвращая ошибки значениями, а не исключениями.

### Таблица прогресса

Отмечайте выполненные задачи:

| Kata | Заготовка прочитана | Реализация | Тесты зелёные (`-DuseSolution` не нужен) | Бенчмарк прогнан |
|---|---|---|---|---|
| 01 StripedLockCache | [ ] | [ ] | [ ] | [ ] |
| 02 AsyncTaskBatcher | [ ] | [ ] | [ ] | — |
| 03 TokenBucketRateLimiter | [ ] | [ ] | [ ] | [ ] |
| 04 RingBuffer | [ ] | [ ] | [ ] | [ ] |
| 05 CustomSpliterator | [ ] | [ ] | [ ] | — |
| 06 OffHeapByteBufferPool | [ ] | [ ] | [ ] | — |
| 07 MiniDependencyInjector | [ ] | [ ] | [ ] | — |
| 08 StructuredScatterGather | [ ] | [ ] | [ ] | — |
| 09 KeyedSequentialExecutor | [ ] | [ ] | [ ] | — |
| 10 TreiberStack | [ ] | [ ] | [ ] | — |
| 11 CircuitBreaker | [ ] | [ ] | [ ] | — |
| 12 ConsistentHashingRouter | [ ] | [ ] | [ ] | [ ] |
| 13 BloomFilter | [ ] | [ ] | [ ] | — |
| 14 ArenaSlabAllocator | [ ] | [ ] | [ ] | [ ] |
| 15 ZeroCopyCsvParser | [ ] | [ ] | [ ] | — |
| 16 HashedWheelTimer | [ ] | [ ] | [ ] | — |
| 17 RadixUrlRouter | [ ] | [ ] | [ ] | — |
| 18 CustomStreamGatherer | [ ] | [ ] | [ ] | — |
| 19 MethodHandleInterceptor | [ ] | [ ] | [ ] | — |
| 20 WriteAheadLog | [ ] | [ ] | [ ] | [ ] |
| 21 LsmMemTableAndSstable | [ ] | [ ] | [ ] | — |
| 22 RaftLeaderElection | [ ] | [ ] | [ ] | — |
| 23 HybridLogicalClock | [ ] | [ ] | [ ] | [ ] |
| 24 OrSetCrdt | [ ] | [ ] | [ ] | — |
| 25 NioReactorEchoServer | [ ] | [ ] | [ ] | — |
| 26 LedgerReconciliation | [ ] | [ ] | [ ] | — |
| 27 IntervalScheduleMerger | [ ] | [ ] | [ ] | — |
| 28 StreamAnalyticsPipeline | [ ] | [ ] | [ ] | — |
| 29 HierarchicalTreeBuilder | [ ] | [ ] | [ ] | — |
| 30 PermissionBitmaskEvaluator | [ ] | [ ] | [ ] | — |
| 31 ResilientDataMapper | [ ] | [ ] | [ ] | — |

> «Тесты зелёные» означает, что ваша реализация в файле заготовки проходит
> `mvn test -Dtest='KataXX*'` **без** `-DuseSolution=true`.

---

## Как работать с kata

1. Прочитайте Javadoc заготовки: там контекст, требования, ограничения и edge-cases.
2. Реализуйте методы в файле заготовки (не в `solutions/`).
3. Запустите тесты kata: `mvn test -Dtest='KataXX*'`.
4. Если тесты зелёные — сверьтесь с эталоном и разбором в `solutions/`.
5. Прогоните бенчмарк, если он есть, и сравните с baseline.
6. Сбросьте заготовку через `git checkout`, чтобы вернуться к упражнению позже.

---

## Диагностика

| Симптом | Причина / решение |
|---|---|
| `UnsupportedOperationException: Not implemented yet` | Так и должно быть: запущены тесты против заготовки. Добавьте `-DuseSolution=true` для проверки эталона. |
| `class file has wrong version` / `preview features are not enabled` | Maven работает не на JDK 21. Проверьте `mvn -version` и выставьте `JAVA_HOME`. |
| `ClassNotFoundException: org.openjdk.jmh.runner.ForkedMain` | `exec:java` не обновил `java.class.path`. `BenchmarkRunner` уже исправляет это; убедитесь, что запускаете именно его. |
| Бенчмарки падают с `UnsupportedOperationException` | Забыли `-DuseSolution=true` (или реализуйте заготовку). |
| Долгий прогон JMH | Учебный профиль — ~10–20 с на бенчмарк. Для черновика уменьшите `iterations` в аннотациях. |
| Тест с TTL «мигает» | TTL-тесты используют Awaitility с запасом; на перегруженной машине увеличьте `atMost(...)`. |

---

## Лицензия

Учебный проект. Используйте свободно для практики и собеседований.
