package dev.kata.solutions.memory;

import dev.kata.memory.Kata15_ZeroCopyCsvParser;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;
import java.util.Objects;

/**
 * Эталонное решение Kata 15.
 *
 * <h2>Один проход, два вида срезов</h2>
 *
 * <p>Парсер сканирует {@link MemorySegment} побайтово. Для обычного поля и для кавычек без
 * экранирования он отдаёт {@code data.asSlice(start, length)} — это <b>view</b> на исходный буфер,
 * данные не копируются. Для поля с {@code ""} логическое значение не является непрерывным куском
 * исходника: две кавычки нужно превратить в одну. Такое поле собирается в переиспользуемый {@link
 * Scratch}, и колбэк получает срез scratch-буфера. Это единственный случай копирования, и он
 * неизбежен при zero-copy API с уже «раскодированным» значением.
 *
 * <p>Scratch создаётся на каждый вызов {@code parse} и растёт амортизированно. Поэтому парсер
 * stateless и потокобезопасен: два потока могут разбирать разные входы одновременно, не деля
 * буферы. Внутри одного вызова срез scratch валиден только до следующего поля — это оговорено в
 * контракте.
 *
 * <h2>Сканирование кавычек</h2>
 *
 * <p>Для quoted-поля выполняется проход до закрывающей кавычки. Пара {@code ""} — экранированная
 * кавычка; одиночная кавычка закрывает поле. После закрывающей кавычки допускается только запятая,
 * {@code \n}, {@code \r} или конец ввода; иначе вход считается malformed. Пробелы и любые другие
 * символы после закрывающей кавычки не «прощаются» — это позволяет не гадать, где заканчивается
 * поле. Внутри кавычек запятые и переводы строк — обычные данные.
 *
 * <h2>Переводы строк и пустые записи</h2>
 *
 * <p>Вне кавычек и {@code \n}, и {@code \r} завершают запись; {@code \r\n} потребляется как один
 * разделитель. Пустая строка (сразу {@code \n} или {@code \r\n}) пропускается: для CSV-выгрузок это
 * обычно случайные пустые строки, а не записи с одной пустой колонкой. Запись {@code ""} при этом
 * не пустая — она содержит одно поле, и колбэк для неё вызывается.
 *
 * <h2>parseLong без String</h2>
 *
 * <p>Число накапливается в отрицательном виде: так корректно обрабатывается {@link Long#MIN_VALUE},
 * у которого нет положительного двойника. Перед каждым умножением проверяется граница; при
 * переполнении или нецифровом символе бросается {@link NumberFormatException}. На успешном пути
 * {@code String}, {@code StringBuilder} и боксинг не создаются.
 */
public final class Kata15_ZeroCopyCsvParserSolution extends Kata15_ZeroCopyCsvParser {

  private static final byte COMMA = ',';
  private static final byte QUOTE = '"';
  private static final byte LF = '\n';
  private static final byte CR = '\r';

  @Override
  public int parse(byte[] data, FieldConsumer consumer) {
    Objects.requireNonNull(data, "data");
    Objects.requireNonNull(consumer, "consumer");
    return parse(MemorySegment.ofArray(data), consumer);
  }

  @Override
  public int parse(MemorySegment data, FieldConsumer consumer) {
    Objects.requireNonNull(data, "data");
    Objects.requireNonNull(consumer, "consumer");

    long length = data.byteSize();
    Scratch scratch = new Scratch();
    int row = 0;
    long position = 0L;

    while (position < length) {
      byte first = data.get(ValueLayout.JAVA_BYTE, position);
      if (first == LF || first == CR) {
        position = skipNewline(data, position, length);
        continue;
      }

      int column = 0;
      while (true) {
        if (position < length && data.get(ValueLayout.JAVA_BYTE, position) == QUOTE) {
          position = parseQuoted(data, position, length, scratch, row, column, consumer);
        } else {
          long start = position;
          while (position < length) {
            byte value = data.get(ValueLayout.JAVA_BYTE, position);
            if (value == COMMA || value == LF || value == CR) {
              break;
            }
            position++;
          }
          consumer.accept(row, column, data.asSlice(start, position - start));
        }

        if (position >= length) {
          row++;
          break;
        }

        byte delimiter = data.get(ValueLayout.JAVA_BYTE, position);
        if (delimiter == COMMA) {
          column++;
          position++;
          // Если запятая — последний символ ввода, следующая итерация отдаст пустое поле.
          continue;
        }

        // delimiter == CR || delimiter == LF
        position = skipNewline(data, position, length);
        row++;
        break;
      }
    }
    return row;
  }

  @Override
  public long parseLong(MemorySegment field) {
    Objects.requireNonNull(field, "field");
    long length = field.byteSize();
    if (length == 0L) {
      throw new NumberFormatException("empty field");
    }

    long index = 0L;
    boolean negative = false;
    byte first = field.get(ValueLayout.JAVA_BYTE, 0L);
    if (first == '+' || first == '-') {
      negative = first == '-';
      index = 1L;
      if (index == length) {
        throw new NumberFormatException("sign without digits");
      }
    }

    long result = 0L;
    for (; index < length; index++) {
      byte value = field.get(ValueLayout.JAVA_BYTE, index);
      if (value < '0' || value > '9') {
        throw new NumberFormatException("non-digit character at offset " + index);
      }
      int digit = value - '0';
      // Накапливаем отрицательное значение: (Long.MIN_VALUE + digit) / 10 — граница,
      // после которой result * 10 - digit выйдет за пределы long.
      if (result < (Long.MIN_VALUE + digit) / 10) {
        throw new NumberFormatException("long overflow at offset " + index);
      }
      result = result * 10L - digit;
    }

    if (!negative) {
      if (result == Long.MIN_VALUE) {
        throw new NumberFormatException("long overflow");
      }
      result = -result;
    }
    return result;
  }

  /**
   * Разбирает quoted-поле и вызывает колбэк.
   *
   * @return позиция сразу после закрывающей кавычки
   */
  private static long parseQuoted(
      MemorySegment data,
      long start,
      long length,
      Scratch scratch,
      int row,
      int column,
      FieldConsumer consumer) {
    long contentStart = start + 1L;
    long scan = contentStart;
    boolean escaped = false;

    while (scan < length) {
      if (data.get(ValueLayout.JAVA_BYTE, scan) == QUOTE) {
        if (scan + 1L < length && data.get(ValueLayout.JAVA_BYTE, scan + 1L) == QUOTE) {
          escaped = true;
          scan += 2L;
          continue;
        }

        long contentEnd = scan;
        long after = scan + 1L;
        if (after < length) {
          byte next = data.get(ValueLayout.JAVA_BYTE, after);
          if (next != COMMA && next != LF && next != CR) {
            throw new IllegalArgumentException(
                "unexpected character after closing quote at offset " + after);
          }
        }

        if (!escaped) {
          consumer.accept(row, column, data.asSlice(contentStart, contentEnd - contentStart));
        } else {
          long rawLength = contentEnd - contentStart;
          if (rawLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("escaped field is too large: " + rawLength);
          }
          scratch.ensure((int) rawLength);
          int out = 0;
          long i = contentStart;
          while (i < contentEnd) {
            byte value = data.get(ValueLayout.JAVA_BYTE, i);
            if (value == QUOTE) {
              // Пары кавычек уже проверены сканированием: здесь всегда "" -> ".
              scratch.set(out++, QUOTE);
              i += 2L;
            } else {
              scratch.set(out++, value);
              i++;
            }
          }
          consumer.accept(row, column, scratch.segment().asSlice(0L, out));
        }
        return after;
      }
      scan++;
    }

    throw new IllegalArgumentException("unterminated quoted field starting at offset " + start);
  }

  private static long skipNewline(MemorySegment data, long position, long length) {
    byte value = data.get(ValueLayout.JAVA_BYTE, position);
    if (value == CR
        && position + 1L < length
        && data.get(ValueLayout.JAVA_BYTE, position + 1L) == LF) {
      return position + 2L;
    }
    return position + 1L;
  }

  /**
   * Переиспользуемый буфер для полей с экранированными кавычками.
   *
   * <p>Создаётся на один вызов {@code parse}; растёт копированием. {@link MemorySegment#ofArray}
   * даёт view на массив, а не копию, поэтому запись в {@code bytes} сразу видна в срезе.
   */
  private static final class Scratch {
    private byte[] bytes = new byte[64];
    private MemorySegment segment = MemorySegment.ofArray(bytes);

    void ensure(int capacity) {
      if (capacity <= bytes.length) {
        return;
      }
      int doubled = bytes.length > Integer.MAX_VALUE / 2 ? Integer.MAX_VALUE : bytes.length * 2;
      int newLength = Math.max(capacity, doubled);
      bytes = Arrays.copyOf(bytes, newLength);
      segment = MemorySegment.ofArray(bytes);
    }

    void set(int index, byte value) {
      bytes[index] = value;
    }

    MemorySegment segment() {
      return segment;
    }
  }
}
