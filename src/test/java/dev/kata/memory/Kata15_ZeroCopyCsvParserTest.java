package dev.kata.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.solutions.memory.Kata15_ZeroCopyCsvParserSolution;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 15.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata15* -DuseSolution=true</pre>
 */
@DisplayName("Kata 15: zero-copy CSV-парсер")
class Kata15_ZeroCopyCsvParserTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata15_ZeroCopyCsvParser newParser() {
    return USE_SOLUTION ? new Kata15_ZeroCopyCsvParserSolution() : new Kata15_ZeroCopyCsvParser();
  }

  @Test
  @DisplayName("Простой CSV: строки, колонки и порядок полей")
  void parsesSimpleCsv() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    List<List<String>> rows = parseAll(parser, "name,age\nAlice,30\nBob,25\n");

    assertThat(rows)
        .containsExactly(List.of("name", "age"), List.of("Alice", "30"), List.of("Bob", "25"));
  }

  @Test
  @DisplayName("Кавычки: запятые и экранированные кавычки внутри поля")
  void quotedCommasAndEscapedQuotes() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    List<List<String>> rows =
        parseAll(parser, "id,note\n1,\"Hello, \"\"world\"\"\"\n2,\"a\"\"b\"\n");

    assertThat(rows)
        .containsExactly(
            List.of("id", "note"), List.of("1", "Hello, \"world\""), List.of("2", "a\"b"));
  }

  @Test
  @DisplayName("Пустые поля и пустые quoted-поля сохраняются")
  void emptyFieldsArePreserved() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    List<List<String>> rows = parseAll(parser, "a,,c\n,,\n\"\"\n");

    assertThat(rows).containsExactly(List.of("a", "", "c"), List.of("", "", ""), List.of(""));
  }

  @Test
  @DisplayName("Поддерживаются \\n и \\r\\n, в том числе внутри кавычек")
  void crlfAndLfAreSupported() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    List<List<String>> rows = parseAll(parser, "a,b\r\nc,d\ne,f\r\n");

    assertThat(rows).containsExactly(List.of("a", "b"), List.of("c", "d"), List.of("e", "f"));
  }

  @Test
  @DisplayName("Многострочное поле в кавычках сохраняет переводы строк")
  void multilineQuotedField() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    List<List<String>> rows =
        parseAll(parser, "id,note\r\n1,\"line1\r\nline2\"\r\n2,\"a\"\"b\r\nc\"\r\n");

    assertThat(rows)
        .containsExactly(
            List.of("id", "note"), List.of("1", "line1\r\nline2"), List.of("2", "a\"b\r\nc"));
  }

  @Test
  @DisplayName("Пустые строки пропускаются, но \"\" — это запись с пустым полем")
  void blankLinesAreSkipped() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    List<List<String>> rows = parseAll(parser, "\n\r\n\r\na,b\n\n\r\nc,d\n");

    assertThat(rows).containsExactly(List.of("a", "b"), List.of("c", "d"));
    assertThat(parser.parse(bytes("\n\n\r\n"), (row, col, field) -> {})).isZero();
  }

  @Test
  @DisplayName("Последняя строка без перевода строки и одиночные поля")
  void lastLineWithoutNewline() {
    Kata15_ZeroCopyCsvParser parser = newParser();

    assertThat(parseAll(parser, "abc")).containsExactly(List.of("abc"));
    assertThat(parseAll(parser, "a,b")).containsExactly(List.of("a", "b"));
    assertThat(parseAll(parser, "a,")).containsExactly(List.of("a", ""));
  }

  @Test
  @DisplayName("Пустой ввод — ноль строк")
  void emptyInput() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    assertThat(parseAll(parser, "")).isEmpty();
    assertThat(parser.parse(new byte[0], (row, col, field) -> {})).isZero();
  }

  @Test
  @DisplayName("Кавычка в середине незакавыченного поля — обычный символ")
  void quoteInsideUnquotedFieldIsLiteral() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    assertThat(parseAll(parser, "a\"b,c")).containsExactly(List.of("a\"b", "c"));
  }

  @Test
  @DisplayName("Некорректные кавычки отвергаются")
  void malformedQuotesAreRejected() {
    Kata15_ZeroCopyCsvParser parser = newParser();

    assertThatThrownBy(() -> parseAll(parser, "\"unterminated"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parseAll(parser, "\"a\"b,c"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parseAll(parser, "\"a\" ,b"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("parseLong: знак, границы long, отсутствие String")
  void parseLongValidValues() {
    Kata15_ZeroCopyCsvParser parser = newParser();

    assertThat(parser.parseLong(ascii("0"))).isZero();
    assertThat(parser.parseLong(ascii("42"))).isEqualTo(42);
    assertThat(parser.parseLong(ascii("-42"))).isEqualTo(-42);
    assertThat(parser.parseLong(ascii("+42"))).isEqualTo(42);
    assertThat(parser.parseLong(ascii("9223372036854775807"))).isEqualTo(Long.MAX_VALUE);
    assertThat(parser.parseLong(ascii("-9223372036854775808"))).isEqualTo(Long.MIN_VALUE);
  }

  @Test
  @DisplayName("parseLong: мусор, пробелы и переполнение отвергаются")
  void parseLongInvalidValues() {
    Kata15_ZeroCopyCsvParser parser = newParser();

    for (String invalid :
        List.of(
            "",
            "+",
            "-",
            " 42",
            "42 ",
            "12x",
            "++1",
            "9223372036854775808",
            "-9223372036854775809")) {
      assertThatThrownBy(() -> parser.parseLong(ascii(invalid)))
          .as("значение '%s'", invalid)
          .isInstanceOf(NumberFormatException.class);
    }
  }

  @Test
  @DisplayName("parseLong работает со срезом внутри большего сегмента")
  void parseLongOnSlice() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    MemorySegment data = MemorySegment.ofArray(bytes("xx-12345yy"));

    assertThat(parser.parseLong(data.asSlice(2, 6))).isEqualTo(-12345);
  }

  @Test
  @DisplayName("Read-only сегмент разбирается, экранирование уходит в scratch")
  void readOnlySegmentIsSupported() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    MemorySegment readOnly = MemorySegment.ofArray(bytes("a,\"b\"\"c\"\n")).asReadOnly();

    List<List<String>> rows = new ArrayList<>();
    parser.parse(
        readOnly,
        (row, col, field) -> {
          while (rows.size() <= row) {
            rows.add(new ArrayList<>());
          }
          rows.get(row).add(text(field));
        });

    assertThat(rows).containsExactly(List.of("a", "b\"c"));
  }

  @Test
  @DisplayName("Перегрузки byte[] и MemorySegment дают одинаковый результат")
  void byteArrayAndSegmentOverloadsAgree() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    String csv = "a,\"b,c\"\r\n1,2\n";

    List<List<String>> fromArray = parseAll(parser, csv);
    List<List<String>> fromSegment = parseAll(parser, MemorySegment.ofArray(bytes(csv)));

    assertThat(fromSegment).isEqualTo(fromArray);
  }

  @Test
  @DisplayName("UTF-8 байты передаются без декодирования, колбэк может декодировать")
  void utf8Content() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    List<List<String>> rows = parseAll(parser, "привет,мир\n");

    assertThat(rows).containsExactly(List.of("привет", "мир"));
  }

  @Test
  @DisplayName("Колбэк получает правильные rowIndex/colIndex в encounter order")
  void callbackIndices() {
    Kata15_ZeroCopyCsvParser parser = newParser();
    List<String> positions = new ArrayList<>();

    parser.parse(
        bytes("a,b\nc,d\n"),
        (row, col, field) -> positions.add(row + ":" + col + "=" + text(field)));

    assertThat(positions).containsExactly("0:0=a", "0:1=b", "1:0=c", "1:1=d");
  }

  @Test
  @DisplayName("Метод parse возвращает число разобранных непустых строк")
  void parseReturnsRowCount() {
    Kata15_ZeroCopyCsvParser parser = newParser();

    assertThat(parser.parse(bytes("a,b\nc,d\n"), (row, col, field) -> {})).isEqualTo(2);
    assertThat(parser.parse(bytes("\n\na,b\n\n"), (row, col, field) -> {})).isEqualTo(1);
    assertThat(parser.parse(bytes(""), (row, col, field) -> {})).isZero();
  }

  @Test
  @DisplayName("null-аргументы запрещены")
  void nullsRejected() {
    Kata15_ZeroCopyCsvParser parser = newParser();

    assertThatThrownBy(() -> parser.parse((byte[]) null, (row, col, field) -> {}))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> parser.parse((MemorySegment) null, (row, col, field) -> {}))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> parser.parse(bytes("a"), null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> parser.parseLong(null)).isInstanceOf(NullPointerException.class);
  }

  private static List<List<String>> parseAll(Kata15_ZeroCopyCsvParser parser, String csv) {
    return parseAll(parser, MemorySegment.ofArray(bytes(csv)));
  }

  private static List<List<String>> parseAll(Kata15_ZeroCopyCsvParser parser, MemorySegment data) {
    List<List<String>> rows = new ArrayList<>();
    parser.parse(
        data,
        (row, col, field) -> {
          while (rows.size() <= row) {
            rows.add(new ArrayList<>());
          }
          rows.get(row).add(text(field));
        });
    return rows;
  }

  private static MemorySegment ascii(String value) {
    return MemorySegment.ofArray(value.getBytes(StandardCharsets.US_ASCII));
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static String text(MemorySegment field) {
    int length = Math.toIntExact(field.byteSize());
    byte[] bytes = new byte[length];
    for (int i = 0; i < length; i++) {
      bytes[i] = field.get(ValueLayout.JAVA_BYTE, i);
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
