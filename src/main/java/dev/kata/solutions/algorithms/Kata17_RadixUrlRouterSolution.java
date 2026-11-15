package dev.kata.solutions.algorithms;

import dev.kata.algorithms.Kata17_RadixUrlRouter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Эталонное решение Kata 17.
 *
 * <h2>Сжатый префиксный бор</h2>
 *
 * <p>Каждый узел хранит не один символ, а целый общий префикс. При вставке {@code /api/users} в
 * пустое дерево создаётся ребро с меткой {@code /api/users}. Вставка {@code /api/orders} находит
 * общий префикс {@code /api/} и <b>расщепляет</b> существующее ребро:
 *
 * <pre>
 *   root ── "/api/" ──┬── "users"
 *                     └── "orders"
 * </pre>
 *
 * <p>Расщепление создаёт промежуточный узел с общей частью; старый узел получает остаток метки.
 * Handler'ы, параметрические и wildcard-дети остаются у старого узла, потому что они относятся к
 * полному (старому) префиксу. Промежуточный узел новых детей не получает. Такая схема даёт O(длины
 * пути) на матчинг и не создаёт узел на каждый символ.
 *
 * <h2>Параметры и wildcard как особые рёбра</h2>
 *
 * <p>{@code {id}} — отдельное ребро {@code paramChild} в узле, где начинается сегмент. Оно
 * захватывает символы до следующего {@code /}. {@code *filepath} — ребро {@code wildcardChild},
 * которое забирает остаток пути целиком. У узла не может быть двух параметрических рёбер: имена в
 * одном дереве должны совпадать, иначе матчинг не знает, какое имя использовать в карте параметров.
 * Конфликт имён проверяется <b>до</b> вставки, поэтому неудачный {@code addRoute} не оставляет
 * дерево полуизменённым.
 *
 * <h2>Приоритет и backtracking</h2>
 *
 * <p>Порядок попыток в узле: статический ребёнок → параметр → wildcard. Если статическая ветка
 * совпала по префиксу, но глубже не нашла handler для запрошенного метода, матчинг возвращается и
 * пробует параметр. Это важно для маршрутов вида {@code /users/me/profile} и {@code /users/{id}}:
 * запрос {@code /users/me} должен попасть во второй, хотя первый начинается с того же префикса.
 *
 * <p>Захваченные параметры складываются в {@link Params}; перед каждой параметрической веткой
 * запоминается размер стека, и при неудаче значения откатываются. Это дешевле копирования карты на
 * каждом шаге.
 *
 * <h2>Синхронизация</h2>
 *
 * <p>{@link ReentrantReadWriteLock}: матчинг — read lock (параллельные чтения), {@code addRoute} —
 * write lock. Дерево не публикуется наружу, поэтому read lock достаточно; copy-on-write дерева был
 * бы дороже при частых обновлениях. Матчинг создаёт {@code String} для каждого захваченного
 * параметра — это неизбежно для API с {@code Map<String, String>}.
 */
public final class Kata17_RadixUrlRouterSolution<T> extends Kata17_RadixUrlRouter<T> {

  private final ReentrantReadWriteLock topologyLock = new ReentrantReadWriteLock();
  private final Node<T> root = new Node<>("");

  @Override
  public void addRoute(String method, String pattern, T handler) {
    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(pattern, "pattern");
    Objects.requireNonNull(handler, "handler");
    if (method.isEmpty()) {
      throw new IllegalArgumentException("method must not be empty");
    }

    List<Token> tokens = tokenize(pattern);

    topologyLock.writeLock().lock();
    try {
      checkConflicts(tokens);
      Node<T> node = root;
      for (Token token : tokens) {
        switch (token.type()) {
          case LITERAL -> node = insertLiteral(node, token.value());
          case PARAM -> {
            if (node.paramChild == null) {
              node.paramChild = new Node<>("");
              node.paramName = token.value();
            } else if (!node.paramName.equals(token.value())) {
              throw new IllegalArgumentException(
                  "conflicting parameter name: " + node.paramName + " vs " + token.value());
            }
            node = node.paramChild;
          }
          case WILDCARD -> {
            if (node.wildcardChild == null) {
              node.wildcardChild = new Node<>("");
              node.wildcardName = token.value();
            } else if (!node.wildcardName.equals(token.value())) {
              throw new IllegalArgumentException(
                  "conflicting wildcard name: " + node.wildcardName + " vs " + token.value());
            }
            node = node.wildcardChild;
          }
        }
      }
      node.handlers.put(method, handler);
    } finally {
      topologyLock.writeLock().unlock();
    }
  }

  @Override
  public Optional<RouteMatch<T>> match(String method, String path) {
    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(path, "path");

    int end = path.length();
    int query = path.indexOf('?');
    if (query >= 0 && query < end) {
      end = query;
    }
    int fragment = path.indexOf('#');
    if (fragment >= 0 && fragment < end) {
      end = fragment;
    }

    topologyLock.readLock().lock();
    try {
      Params params = new Params();
      T handler = matchNode(root, method, path, 0, end, params);
      if (handler == null) {
        return Optional.empty();
      }
      return Optional.of(new RouteMatch<>(handler, params.toMap()));
    } finally {
      topologyLock.readLock().unlock();
    }
  }

  /**
   * Рекурсивный матчинг. {@code index} — сколько символов пути уже потреблено.
   *
   * <p>Порядок веток задаёт приоритет: static -> param -> wildcard. Возврат {@code null} означает,
   * что ветка не дала handler'а для этого метода; вызывающий откатывает свои параметры и пробует
   * следующую ветку.
   */
  private T matchNode(Node<T> node, String method, String path, int index, int end, Params params) {
    if (index == end) {
      T exact = node.handlers.get(method);
      if (exact != null) {
        return exact;
      }
      if (node.wildcardChild != null) {
        T wildcard = node.wildcardChild.handlers.get(method);
        if (wildcard != null) {
          params.put(node.wildcardName, "");
          return wildcard;
        }
      }
      return null;
    }

    // 1. Точный статический префикс.
    Node<T> staticChild = node.children.get(path.charAt(index));
    if (staticChild != null) {
      int matched = commonPrefixLength(staticChild.prefix, path, index, end);
      if (matched == staticChild.prefix.length()) {
        T handler = matchNode(staticChild, method, path, index + matched, end, params);
        if (handler != null) {
          return handler;
        }
      }
    }

    // 2. Именованный параметр: ровно один непустой сегмент.
    if (node.paramChild != null) {
      int segmentEnd = index;
      while (segmentEnd < end && path.charAt(segmentEnd) != '/') {
        segmentEnd++;
      }
      if (segmentEnd > index) {
        int mark = params.mark();
        params.put(node.paramName, path.substring(index, segmentEnd));
        T handler = matchNode(node.paramChild, method, path, segmentEnd, end, params);
        if (handler != null) {
          return handler;
        }
        params.rollback(mark);
      }
    }

    // 3. Wildcard: весь остаток пути, включая '/'.
    if (node.wildcardChild != null) {
      T handler = node.wildcardChild.handlers.get(method);
      if (handler != null) {
        params.put(node.wildcardName, path.substring(index, end));
        return handler;
      }
    }

    return null;
  }

  /**
   * Вставляет литерал в сжатый бор и возвращает узел, соответствующий концу литерала.
   *
   * <p>Если существующее ребро частично совпало, оно расщепляется: создаётся промежуточный узел с
   * общим префиксом, старый узел получает остаток. Все handler'ы и особые дети остаются у старого
   * узла — они относятся к полному старому префиксу.
   */
  private Node<T> insertLiteral(Node<T> node, String literal) {
    if (literal.isEmpty()) {
      return node;
    }

    char first = literal.charAt(0);
    Node<T> child = node.children.get(first);
    if (child == null) {
      Node<T> created = new Node<>(literal);
      node.children.put(first, created);
      return created;
    }

    String childPrefix = child.prefix;
    int common = commonPrefixLength(literal, childPrefix);
    if (common == childPrefix.length()) {
      return insertLiteral(child, literal.substring(common));
    }

    Node<T> middle = new Node<>(childPrefix.substring(0, common));
    child.prefix = childPrefix.substring(common);
    middle.children.put(child.prefix.charAt(0), child);
    node.children.put(first, middle);

    if (common == literal.length()) {
      return middle;
    }
    return insertLiteral(middle, literal.substring(common));
  }

  /**
   * Проверяет конфликты имён параметров/wildcard до изменения дерева.
   *
   * <p>Идёт по существующим рёбрам, пока путь совпадает; как только начинается новый участок,
   * дальнейших конфликтов быть не может. Благодаря этому неудачный {@code addRoute} атомарен:
   * дерево ещё не изменено.
   */
  private void checkConflicts(List<Token> tokens) {
    Node<T> node = root;
    for (Token token : tokens) {
      switch (token.type()) {
        case LITERAL -> {
          String literal = token.value();
          while (!literal.isEmpty()) {
            Node<T> child = node.children.get(literal.charAt(0));
            if (child == null) {
              return;
            }
            int common = commonPrefixLength(literal, child.prefix);
            if (common != child.prefix.length()) {
              return; // здесь будет расщепление — дальше всё новое
            }
            node = child;
            literal = literal.substring(common);
          }
        }
        case PARAM -> {
          if (node.paramChild == null) {
            return;
          }
          if (!node.paramName.equals(token.value())) {
            throw new IllegalArgumentException(
                "conflicting parameter name: " + node.paramName + " vs " + token.value());
          }
          node = node.paramChild;
        }
        case WILDCARD -> {
          if (node.wildcardChild == null) {
            return;
          }
          if (!node.wildcardName.equals(token.value())) {
            throw new IllegalArgumentException(
                "conflicting wildcard name: " + node.wildcardName + " vs " + token.value());
          }
          node = node.wildcardChild;
        }
      }
    }
  }

  /** Разбирает pattern на литералы, параметры и wildcard, валидируя синтаксис. */
  private static List<Token> tokenize(String pattern) {
    if (pattern.isEmpty()) {
      throw new IllegalArgumentException("pattern must not be empty");
    }
    if (pattern.charAt(0) != '/') {
      throw new IllegalArgumentException("pattern must start with '/': " + pattern);
    }
    if (pattern.indexOf('?') >= 0 || pattern.indexOf('#') >= 0) {
      throw new IllegalArgumentException("pattern must not contain query/fragment: " + pattern);
    }

    List<Token> tokens = new ArrayList<>();
    StringBuilder literal = new StringBuilder();
    Set<String> parameterNames = new HashSet<>();
    int index = 0;

    while (index < pattern.length()) {
      char current = pattern.charAt(index);
      if (current == '{' || current == '*') {
        if (index == 0 || pattern.charAt(index - 1) != '/') {
          throw new IllegalArgumentException(
              "parameter/wildcard must start a segment at index " + index + ": " + pattern);
        }
        if (literal.length() > 0) {
          tokens.add(new Token(TokenType.LITERAL, literal.toString()));
          literal.setLength(0);
        }

        if (current == '{') {
          int close = pattern.indexOf('}', index + 1);
          if (close < 0) {
            throw new IllegalArgumentException("unclosed parameter in pattern: " + pattern);
          }
          String name = pattern.substring(index + 1, close);
          validateName(name, "parameter", pattern);
          if (!parameterNames.add(name)) {
            throw new IllegalArgumentException("duplicate parameter name: " + name);
          }
          if (close + 1 < pattern.length() && pattern.charAt(close + 1) != '/') {
            throw new IllegalArgumentException(
                "unexpected text after parameter at index " + (close + 1) + ": " + pattern);
          }
          tokens.add(new Token(TokenType.PARAM, name));
          index = close + 1;
        } else {
          String name = pattern.substring(index + 1);
          validateName(name, "wildcard", pattern);
          tokens.add(new Token(TokenType.WILDCARD, name));
          index = pattern.length();
        }
      } else {
        literal.append(current);
        index++;
      }
    }

    if (literal.length() > 0) {
      tokens.add(new Token(TokenType.LITERAL, literal.toString()));
    }
    return tokens;
  }

  private static void validateName(String name, String kind, String pattern) {
    if (name.isEmpty()) {
      throw new IllegalArgumentException("empty " + kind + " name in pattern: " + pattern);
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-')) {
        throw new IllegalArgumentException(
            "invalid character in " + kind + " name '" + name + "': " + pattern);
      }
    }
  }

  private static int commonPrefixLength(String left, String right) {
    int limit = Math.min(left.length(), right.length());
    int i = 0;
    while (i < limit && left.charAt(i) == right.charAt(i)) {
      i++;
    }
    return i;
  }

  private static int commonPrefixLength(String prefix, String path, int offset, int end) {
    int limit = Math.min(prefix.length(), end - offset);
    int i = 0;
    while (i < limit && prefix.charAt(i) == path.charAt(offset + i)) {
      i++;
    }
    return i;
  }

  private enum TokenType {
    LITERAL,
    PARAM,
    WILDCARD
  }

  private record Token(TokenType type, String value) {}

  /** Стек захваченных параметров с откатом при backtracking. */
  private static final class Params {
    private final Map<String, String> values = new LinkedHashMap<>();
    private final List<String> stack = new ArrayList<>();

    int mark() {
      return stack.size();
    }

    void put(String name, String value) {
      values.put(name, value);
      stack.add(name);
    }

    void rollback(int mark) {
      while (stack.size() > mark) {
        int last = stack.size() - 1;
        values.remove(stack.remove(last));
      }
    }

    Map<String, String> toMap() {
      // RouteMatch сам делает неизменяемую копию; здесь достаточно изменяемого снимка.
      return values.isEmpty() ? Map.of() : new LinkedHashMap<>(values);
    }
  }

  private static final class Node<T> {
    String prefix;
    final Map<Character, Node<T>> children = new HashMap<>();
    Node<T> paramChild;
    String paramName;
    Node<T> wildcardChild;
    String wildcardName;
    final Map<String, T> handlers = new HashMap<>();

    Node(String prefix) {
      this.prefix = prefix;
    }
  }
}
