# CLAUDE.md — Инструкции для проекта `java-kata`

## О проекте
`java-kata` — набор практических ежедневных упражнений (katas) уровня Senior/Principal Java Engineer на чистом JDK 21 (с `--enable-preview`) без тяжелых фреймворков. В проекте 71 задача в семи волнах: 31 основная kata (concurrency, memory, storage, distributed systems, networking, prod-ready domain logic), 30 задач утренней алгоритмической разминки LeetCode и 10 коротких Easy Kata на идиоматичный JCF/Stream API.

## Архитектурные стандарты (обязательно для всех новых задач)
1. **Структура одной каты (строго 3 файла):**
    - `src/main/java/dev/kata/<topic>/KataXX_<Name>.java` — заготовка с подробным Javadoc на русском языке (инженерный контекст, требования, сложность Big-O, edge-cases) и методами, выбрасывающими `UnsupportedOperationException("Not implemented yet")`.
    - `src/main/java/dev/kata/solutions/<topic>/KataXX_<Name>Solution.java` — эталонное решение Senior-уровня с комментариями на русском языке (обоснование структур данных, конкурентности и аллокаций).
    - `src/test/java/dev/kata/<topic>/KataXX_<Name>Test.java` — юнит- и стресс-тесты с `@DisplayName` на русском языке.
2. **Переключение режима тестов:**
    - По умолчанию `mvn test` запускает тесты на заготовке `KataXX_<Name>` (тесты должны падать с `UnsupportedOperationException`).
    - При передаче `-DuseSolution=true` те же тесты должны запускаться на `KataXX_<Name>Solution` и проходить на 100%.
3. **Запрет на внешние зависимости:**
    - В `src/main` разрешен **только стандартный JDK 21** (и SLF4J при необходимости). Запрещено добавлять Guava, Apache Commons и сторонние библиотеки — все алгоритмы, хеш-функции и структуры данных реализуются самостоятельно.

## Prod-Ready Craft (Collections, Streams & Domain Logic)
Волна 5 — задачи **Kata26–Kata31** в пакете `dev.kata.prod`. Это не алгоритмические головоломки, а прикладная доменная логика, которую пишут каждый день:

| Kata | Тема | Главный навык |
|---|---|---|
| 26 `LedgerReconciliation` | Сверка платежей | `BigDecimal.compareTo`, `Currency`, допуски по времени, grace period |
| 27 `IntervalScheduleMerger` | Расписание занятости | `TreeMap`/`NavigableMap`, floor/ceiling, слияние и разрезание интервалов |
| 28 `StreamAnalyticsPipeline` | Аналитика заказов | `Collectors.teeing`/`groupingBy`/`flatMapping`, один проход, детерминированные ничьи |
| 29 `HierarchicalTreeBuilder` | Дерево каталога | Плоский список → лес за O(N), итеративные обходы, циклы и сироты |
| 30 `PermissionBitmaskEvaluator` | RBAC + ABAC | `EnumSet`/`EnumMap` как битовая маска, наследование ролей, DENY wins, volatile-снимок |
| 31 `ResilientDataMapper` | Валидация импорта | Applicative `combine` с накоплением ошибок vs короткозамыкающий `flatMap` |

Требования к новым задачам этого направления:
- только стандартный JDK 21: `java.util`, Stream API, `java.time`, records, pattern matching;
- доменные инварианты — в records (компактные конструкторы, защитные копии);
- ожидаемые ошибки валидации — значения, а не исключения; исключения только для нарушения контракта;
- конкурентность — copy-on-write снимок + `volatile`, чтение без lock;
- тесты обязаны покрывать пустые входы, границы, порядок ошибок и иммутабельность возвращаемых коллекций.

## Morning Warm-Up (Wave 6)
30 задач LeetCode (#1–#700), разбитые на 4 недели по 7/7/7/9. Это ежедневная разминка перед основной работой: короткие задачи, но с обязательным разбором сложности и идиоматичным JDK-решением.

**Раскладка и именование:**
- заготовка — `src/main/java/dev/kata/warmup/weekN/Warmup<N>_<NN>_<Name>.java`;
- эталон — `src/main/java/dev/kata/solutions/warmup/weekN/Warmup<N>_<NN>_<Name>Solution.java`;
- тест — `src/test/java/dev/kata/warmup/weekN/Warmup<N>_<NN>_<Name>Test.java`;
- общие структуры — `dev.kata.warmup.common.ListNode` и `dev.kata.warmup.common.TreeNode`; тесты используют `ListNode.of(...)`/`toList()` и `TreeNode.of(...)`/`TreeNode.toList(...)` вместо ручного связывания узлов.

**Требования к Javadoc заготовки:** номер задачи LeetCode, условие на русском, примеры входа/выхода, ограничения, целевая сложность по времени и памяти (Big-O), ключевая идея решения.

**Требования к эталону:** именно идиоматичный приём из условия, а не «работающий перебор»:
- Бойер-Мур для majority, Флойд для happy number и цикла в списке;
- `n & (n - 1)` для веса Хэмминга, XOR для single/missing number, `>>>` для reverse bits;
- `switch` expression для FizzBuzz и римских цифр, `IntStream.rangeClosed` для FizzBuzz;
- KMP/LPS для поиска подстроки, два указателя без аллокаций для строк и массивов;
- dummy-узел для слияния списков, `ArrayDeque` вместо `Stack` для итеративного in-order;
- O(1) памяти там, где это требуется (move zeroes, merge sorted array, climbing stairs, Floyd).

**Тесты:** LeetCode-примеры, границы (пустой вход, один элемент, дубликаты, отрицательные значения, границы `int`), проверка in-place мутации и возвращаемых поддеревьев. Для циклических списков тест обязан строить реальный цикл, а не имитировать его флагом.

Проверка недели: `mvn test -Dtest='Warmup1*' -DuseSolution=true`. Прогресс отмечается чекбоксами в README-трекере на 4 недели.

## Easy Kata: JCF & Functional Style (`Easy01` — `Easy10`)
10 лаконичных задач для ежедневной 5–10-минутной практики идиоматичного Java Collections Framework, Stream API и Java 21 в декларативном стиле.

**Раскладка:** заготовки — `src/main/java/dev/kata/easy/EasyNN_<Name>.java`, эталоны — `src/main/java/dev/kata/solutions/easy/EasyNN_<Name>Solution.java`, тесты — `src/test/java/dev/kata/easy/EasyNN_<Name>Test.java`. Именование сквозное: `Easy01`–`Easy10` без номеров волн.

**Требования к эталонам:**
- никакого «тяжёлого» императива: вместо ручных циклов с мутацией индексов — `Stream`, `Collectors`, `Map.merge`, `Map.computeIfAbsent`, цепочки `Comparator`, `ArrayDeque`, `SequencedCollection`/`SequencedSet`;
- только базовый JCF: `ArrayList`, `HashMap`, `LinkedHashMap`, `HashSet`, `LinkedHashSet`, `ArrayDeque`;
- декларативность и читаемость важнее микрооптимизаций; сложность всё равно указывается в Javadoc;
- возвращаемые коллекции неизменяемы (`List.copyOf`, `Set.copyOf`, `toList()`, `unmodifiableMap`);
- эталонные решения объясняют выбор структуры и приёма по-русски.

**Задачи:** частоты и топ-K на `groupingBy/counting`; группировка сотрудников на `mapping`/`averagingInt`; diff коллекций через `Set` индексов; обратный индекс тегов через `flatMap`; слияние балансов через `Map.merge`; скобки на `ArrayDeque`; LRU на `LinkedHashMap(accessOrder=true)`; chunk/zip на `IntStream`; история на `SequencedSet.addFirst/removeLast`; многокритериальная сортировка цепочкой `Comparator`.

Проверка: `mvn test -Dtest='Easy*' -DuseSolution=true`. Прогресс отмечается чекбоксами в README-разделе Easy Kata.

## Команды сборки и проверки
- Проверка всех эталонных решений: `mvn test -DuseSolution=true`
- Проверка конкретной каты (эталон): `mvn test -Dtest=Kata08* -DuseSolution=true`
- Проверка конкретной каты (заготовка): `mvn test -Dtest=Kata08*`
- Проверка недели разминки: `mvn test -Dtest='Warmup1*' -DuseSolution=true`
- Проверка Easy Kata: `mvn test -Dtest='Easy*' -DuseSolution=true`
- Форматирование кода: `mvn spotless:apply`
- Запуск JMH-бенчмарка: `mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" -Dexec.args="<BenchmarkName>"`

## Правила завершения работы (Definition of Done)
При добавлении новых задач всегда выполняй перед коммитом:
1. Обнови таблицу прогресса в `README.md`.
2. Запусти `mvn test -DuseSolution=true` — все тесты проекта должны быть зелеными.
3. Запусти `mvn spotless:apply`.
4. Сделай осмысленный git-коммит.
