# CLAUDE.md — Инструкции для проекта `java-kata`

## О проекте
`java-kata` — набор практических ежедневных упражнений (katas) уровня Senior/Principal Java Engineer на чистом JDK 21 (с `--enable-preview`) без тяжелых фреймворков. В проекте 31 задача в пяти волнах: concurrency, memory, storage, distributed systems, networking и prod-ready domain logic.

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

## Команды сборки и проверки
- Проверка всех эталонных решений: `mvn test -DuseSolution=true`
- Проверка конкретной каты (эталон): `mvn test -Dtest=Kata08* -DuseSolution=true`
- Проверка конкретной каты (заготовка): `mvn test -Dtest=Kata08*`
- Форматирование кода: `mvn spotless:apply`
- Запуск JMH-бенчмарка: `mvn test-compile exec:java -Dexec.mainClass="dev.kata.benchmarks.BenchmarkRunner" -Dexec.args="<BenchmarkName>"`

## Правила завершения работы (Definition of Done)
При добавлении новых задач всегда выполняй перед коммитом:
1. Обнови таблицу прогресса в `README.md`.
2. Запусти `mvn test -DuseSolution=true` — все тесты проекта должны быть зелеными.
3. Запусти `mvn spotless:apply`.
4. Сделай осмысленный git-коммит.
