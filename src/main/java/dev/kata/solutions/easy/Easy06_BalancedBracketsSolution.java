package dev.kata.solutions.easy;

import dev.kata.easy.Easy06_BalancedBrackets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Эталон Easy 06.
 *
 * <p>Открывающая скобка кладётся в стек. Закрывающая ищется в {@link #MATCHING}; если это не
 * скобка, символ игнорируется. Для закрывающей стек не должен быть пуст, а верхний элемент должен
 * совпадать с ожидаемой парой. В конце стек обязан быть пуст — иначе остались незакрытые скобки.
 *
 * <p>Отдельный {@code Set} открывающих скобок убирает цепочку {@code if (ch == '(' || ...)}, а
 * {@code Map.of} заменяет {@code switch}. Ошибка на любой позиции возвращает {@code false} сразу.
 */
public final class Easy06_BalancedBracketsSolution extends Easy06_BalancedBrackets {

  private static final Set<Character> OPENING = Set.of('(', '[', '{');
  private static final Map<Character, Character> MATCHING = Map.of(')', '(', ']', '[', '}', '{');

  @Override
  public boolean isBalanced(String input) {
    Objects.requireNonNull(input, "input");

    Deque<Character> stack = new ArrayDeque<>();
    for (int i = 0; i < input.length(); i++) {
      char symbol = input.charAt(i);
      if (OPENING.contains(symbol)) {
        stack.push(symbol);
      } else if (MATCHING.containsKey(symbol)) {
        if (stack.isEmpty() || stack.pop() != MATCHING.get(symbol)) {
          return false;
        }
      }
    }
    return stack.isEmpty();
  }
}
