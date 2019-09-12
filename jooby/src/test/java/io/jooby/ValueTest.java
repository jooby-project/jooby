package io.jooby;

import io.jooby.internal.UrlParser;
import io.jooby.internal.ValueConverterHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValueTest {

  @Test
  public void simpleQueryString() {
    queryString("&foo=bar", queryString -> {
      assertEquals("?&foo=bar", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });
    queryString("foo=bar&", queryString -> {
      assertEquals("?foo=bar&", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });
    queryString("foo=bar&&", queryString -> {
      assertEquals("?foo=bar&&", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });

    queryString("foo=bar", queryString -> {
      assertEquals("?foo=bar", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });
    queryString("foo=bar", queryString -> {
      assertEquals("?foo=bar", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });
    queryString("a=1&b=2", queryString -> {
      assertEquals("?a=1&b=2", queryString.queryString());
      assertEquals(1, queryString.get("a").intValue());
      assertEquals(2, queryString.get("b").intValue());
      assertEquals(2, queryString.size());
    });
    queryString("a=1&b=2&", queryString -> {
      assertEquals("?a=1&b=2&", queryString.queryString());
      assertEquals(1, queryString.get("a").intValue());
      assertEquals(2, queryString.get("b").intValue());
      assertEquals(2, queryString.size());
    });
    queryString("a=1&&b=2&", queryString -> {
      assertEquals("?a=1&&b=2&", queryString.queryString());
      assertEquals(1, queryString.get("a").intValue());
      assertEquals(2, queryString.get("b").intValue());
      assertEquals(2, queryString.size());
    });
    queryString("a=1&a=2", queryString -> {
      assertEquals("?a=1&a=2", queryString.queryString());
      assertEquals(1, queryString.get("a").get(0).intValue());
      assertEquals(2, queryString.get("a").get(1).intValue());
      assertEquals(2, queryString.get("a").size());
      assertEquals(1, queryString.size());
    });
    queryString("a=1;a=2", queryString -> {
      assertEquals("?a=1;a=2", queryString.queryString());
      assertEquals(1, queryString.get("a").get(0).intValue());
      assertEquals(2, queryString.get("a").get(1).intValue());
      assertEquals(2, queryString.get("a").size());
      assertEquals(1, queryString.size());
    });
    queryString("a=", queryString -> {
      assertEquals("?a=", queryString.queryString());
      assertEquals("", queryString.get("a").value());
      assertEquals(1, queryString.size());
    });
    queryString("a=&", queryString -> {
      assertEquals("?a=&", queryString.queryString());
      assertEquals("", queryString.get("a").value());
      assertEquals(1, queryString.size());
    });
    queryString("a=&&", queryString -> {
      assertEquals("?a=&&", queryString.queryString());
      assertEquals("", queryString.get("a").value());
      assertEquals(1, queryString.size());
    });
    queryString("", queryString -> {
      assertEquals("", queryString.queryString());
      assertEquals(0, queryString.size());
    });
    queryString(null, queryString -> {
      assertEquals("", queryString.queryString());
      assertEquals(0, queryString.size());
    });
  }

  @Test
  public void dotNotation() {
    queryString("user.name=root&user.pwd=pass", queryString -> {
      assertEquals("?user.name=root&user.pwd=pass", queryString.queryString());
      assertEquals(1, queryString.size());
      assertEquals(2, queryString.get("user").size());
      assertEquals("root", queryString.get("user").get("name").value());
      assertEquals("pass", queryString.get("user").get("pwd").value());
    });

    queryString("user[name]=root&user[pwd]=pass", queryString -> {
      assertEquals("?user[name]=root&user[pwd]=pass", queryString.queryString());
      assertEquals(1, queryString.size());
      assertEquals(2, queryString.get("user").size());
      assertEquals("root", queryString.get("user").get("name").value());
      assertEquals("pass", queryString.get("user").get("pwd").value());
    });

    queryString("0.name=root&0.pwd=pass", queryString -> {
      assertEquals("?0.name=root&0.pwd=pass", queryString.queryString());
      assertEquals(1, queryString.size());
      assertEquals(2, queryString.get(0).size());
      assertEquals("root", queryString.get(0).get("name").value());
      assertEquals("pass", queryString.get(0).get("pwd").value());
    });

    queryString(
        "user.name=edgar&user.address.street=Street&user.address.number=55&user.type=dev",
        queryString -> {
          assertEquals(
              "?user.name=edgar&user.address.street=Street&user.address.number=55&user.type=dev",
              queryString.queryString());
          assertEquals(1, queryString.size());
          assertEquals(3, queryString.get("user").size());
          assertEquals("edgar", queryString.get("user").get("name").value());
          assertEquals("dev", queryString.get("user").get("type").value());
          assertEquals(2, queryString.get("user").get("address").size());
          assertEquals("Street",
              queryString.get("user").get("address").get("street").value());
          assertEquals("55", queryString.get("user").get("address").get("number").value());
        });
  }

  @Test
  public void bracketNotation() {
    queryString("a[b]=1&a[c]=2", queryString -> {
      assertEquals("?a[b]=1&a[c]=2", queryString.queryString());
      assertEquals(1, queryString.size());
      assertEquals(1, queryString.get("a").get("b").intValue());
      assertEquals(2, queryString.get("a").get("c").intValue());
    });

    queryString(
        "username=xyz&address[country][name]=AR&address[line1]=Line1&address[country][city]=BA",
        queryString -> {
          assertEquals(
              "?username=xyz&address[country][name]=AR&address[line1]=Line1&address[country][city]=BA",
              queryString.queryString());
          assertEquals(2, queryString.size());
          assertEquals("xyz", queryString.get("username").value());
          assertEquals("AR", queryString.get("address").get("country").get("name").value());
          assertEquals("BA", queryString.get("address").get("country").get("city").value());
          assertEquals("Line1", queryString.get("address").get("line1").value());
          assertEquals("{username=xyz, address={country={name=AR, city=BA}, line1=Line1}}",
              queryString.toString());
        });

    //    queryString("?list=1,2,3", queryString -> {
    //      assertEquals("?list=1,2,3", queryString.queryString());
    //      assertEquals(1, queryString.size());
    //      assertEquals(1, queryString.get("list").get(0).intValue());
    //      assertEquals(2, queryString.get("list").get(1).intValue());
    //      assertEquals(3, queryString.get("list").get(2).intValue());
    //      assertEquals("{list=[1, 2, 3]}", queryString.toString());
    //    });
  }

  @Test
  public void arrayArity() {
    assertEquals("1", Value.value(null, "a", "1").value());
    assertEquals("1", Value.value(null, "a", "1").get(0).value());
    assertEquals(1, Value.value(null, "a", "1").size());
    queryString("a=1&a=2", queryString -> {
      assertEquals("1", queryString.get("a").get(0).value());
      assertEquals("2", queryString.get("a").get(1).value());
    });
  }

  @Test
  public void valueToMap() {
    queryString("foo=bar", queryString -> {
      assertEquals("{foo=[bar]}", queryString.toMultimap().toString());
    });
    queryString("a=1;a=2", queryString -> {
      assertEquals("{a=[1, 2]}", queryString.toMultimap().toString());
    });
    queryString(
        "username=xyz&address[country][name]=AR&address[line1]=Line1&address[country][city]=BA",
        queryString -> {
          assertEquals(
              "{username=[xyz], address.country.name=[AR], address.country.city=[BA], address.line1=[Line1]}",
              queryString.toMultimap().toString());
          assertEquals(
              "{address.country.name=[AR], address.country.city=[BA], address.line1=[Line1]}",
              queryString.get("address").toMultimap().toString());
          assertEquals("{country.name=[AR], country.city=[BA]}",
              queryString.get("address").get("country").toMultimap().toString());
          assertEquals("{city=[BA]}",
              queryString.get("address").get("country").get("city").toMultimap().toString());
        });
  }

  @Test
  public void verifyIllegalAccess() {
    /** Object: */
    queryString("foo=bar", queryString -> {
      assertThrows(MissingValueException.class,
          () -> queryString.get("a").get("a").get("a").value());
      assertThrows(MissingValueException.class, () -> queryString.get("missing").value());
      assertThrows(MissingValueException.class, () -> queryString.get(0).value());
      assertEquals("missing", queryString.get("missing").value("missing"));
      assertEquals("a", queryString.get("a").get("a").get("a").value("a"));
    });

    /** Array: */
    queryString("a=1;a=2", queryString -> {
      assertThrows(BadRequestException.class, () -> queryString.get("a").value());
      assertEquals("1", queryString.get("a").get(0).value());
      assertEquals("2", queryString.get("a").get(1).value());
      assertThrows(MissingValueException.class, () -> queryString.get("a").get("b").value());
      assertThrows(MissingValueException.class, () -> queryString.get("a").get(3).value());
      assertEquals("missing", queryString.get("a").get(3).value("missing"));
    });

    /** Single Property: */
    queryString("foo=bar", queryString -> {
      assertThrows(MissingValueException.class,
          () -> queryString.get("foo").get("missing").value());
      assertEquals("bar", queryString.get("foo").get(0).value());
    });

    /** Missing Property: */
    queryString("", queryString -> {
      assertThrows(MissingValueException.class,
          () -> queryString.get("foo").get("missing").value());
      assertThrows(MissingValueException.class, () -> queryString.get("foo").get(0).value());
    });
  }

  @Test
  public void decode() {
    queryString("name=Pedro%20Picapiedra", queryString -> {
      assertEquals("Pedro Picapiedra", queryString.get("name").value());
    });

    queryString("file=js%2Findex.js", queryString -> {
      assertEquals("js/index.js", queryString.get("file").value());
    });

    queryString("25=%20%25", queryString -> {
      assertEquals(" %", queryString.get("25").value());
    });

    queryString("plus=a+b", queryString -> {
      assertEquals("a b", queryString.get("plus").value());
    });
    queryString("tail=a%20%2B", queryString -> {
      assertEquals("a +", queryString.get("tail").value());
    });
  }

  @Test
  public void empty() {
    queryString("n&x=&&", queryString -> {
      assertEquals("", queryString.get("n").value());
      assertEquals("", queryString.get("x").value());
      assertEquals(Collections.singletonList(""), queryString.get("n").toList());
      assertEquals(Collections.singletonList(""), queryString.get("x").toList());
      Map<String, String> map = new HashMap<>();
      map.put("n", "");
      map.put("x", "");
      assertEquals(map, queryString.toMap());
    });
  }

  @Test
  public void customMapper() {
    assertEquals(new BigDecimal("3.14"), Value.value(null, "n", "3.14").value(BigDecimal::new));
    SneakyThrows.Function<String, BigDecimal> toBigDecimal = BigDecimal::new;
    assertMessage(NumberFormatException.class,
        () -> Value.value(null, "n", "x").value(toBigDecimal), null);
  }

  @Test
  public void toCollection() {
    /** Array: */
    queryString("a=1;a=2;a=1", queryString -> {
      assertEquals(Arrays.asList("1", "2", "1"), queryString.get("a").toList());

      assertEquals(new LinkedHashSet<>(Arrays.asList("1", "2")), queryString.get("a").toSet());
    });

    queryString("a=1", queryString -> {
      assertEquals(Arrays.asList("1"), queryString.get("a").toList());
    });
    queryString("a.b=1;a.b=2", queryString -> {
      assertEquals(Arrays.asList("1", "2"), queryString.get("a").get("b").toList());
    });
    /** Single: */
    assertEquals(Arrays.asList("1"), Value.value(null, "a", "1").toList());
    /** Missing: */
    assertEquals(Collections.emptyList(), Value.missing("a").toList());
  }

  @Test
  public void toOptional() {
    /** Array: */
    queryString("a=1;a=2", queryString -> {
      assertMessage(BadRequestException.class, () -> queryString.get("a").toOptional(),
          "Cannot convert value: 'a', to: 'java.lang.String'");
      assertEquals(Optional.of("1"), queryString.get("a").get(0).toOptional());
      assertEquals(Optional.empty(), queryString.get("a").get(2).toOptional());
    });
  }

  enum Letter {
    A, B
  }

  @Test
  public void toEnum() {
    /** Array: */
    queryString("e=a&;e=B", queryString -> {
      assertEquals(Letter.A, queryString.get("e").get(0).toEnum(Letter::valueOf));
      assertEquals(Letter.B, queryString.get("e").get(1).toEnum(Letter::valueOf));
      assertMessage(MissingValueException.class,
          () -> queryString.get("e").get(2).toEnum(Letter::valueOf),
          "Missing value: 'e[2]'");
    });
  }

  @Test
  public void verifyExceptionMessage() {
    /** Object: */
    queryString("foo=bar", queryString -> {
      assertMessage(BadRequestException.class, () -> queryString.get("foo").intValue(),
          "Cannot convert value: 'foo', to: 'int'");
      assertMessage(BadRequestException.class, () -> queryString.get("foo").intValue(0),
          "Cannot convert value: 'foo', to: 'int'");
      assertMessage(MissingValueException.class, () -> queryString.get("foo").get("bar").value(),
          "Missing value: 'foo.bar'");
      assertMessage(MissingValueException.class, () -> queryString.get("foo").get(1).value(),
          "Missing value: 'foo.1'");
      assertMessage(MissingValueException.class, () -> queryString.get("r").longValue(),
          "Missing value: 'r'");
      assertEquals(1, queryString.get("a").intValue(1));
    });

    /** Array: */
    queryString("a=b;a=c", queryString -> {
      assertMessage(BadRequestException.class, () -> queryString.get("a").value(),
          "Cannot convert value: 'a', to: 'java.lang.String'");
      assertMessage(BadRequestException.class, () -> queryString.get("a").get(0).longValue(),
          "Cannot convert value: 'a', to: 'long'");
      assertMessage(MissingValueException.class, () -> queryString.get("a").get(3).longValue(),
          "Missing value: 'a[3]'");
      assertMessage(MissingValueException.class, () -> queryString.get("a").get("b").value(),
          "Missing value: 'a.b'");
      assertMessage(MissingValueException.class,
          () -> queryString.get("a").get("b").get(3).longValue(),
          "Missing value: 'a.b[3]'");
    });

    /** Single: */
    assertMessage(BadRequestException.class, () -> Value.value(null, "foo", "bar").intValue(),
        "Cannot convert value: 'foo', to: 'int'");

    assertMessage(MissingValueException.class, () -> Value.value(null, "foo", "bar").get("foo").value(),
        "Missing value: 'foo.foo'");

    assertMessage(MissingValueException.class, () -> Value.value(null, "foo", "bar").get(1).value(),
        "Missing value: 'foo.1'");
  }

  public static <T extends Throwable> void assertMessage(Class<T> expectedType,
      Executable executable, String message) {
    T x = assertThrows(expectedType, executable);
    if (message != null) {
      assertEquals(message, x.getMessage());
    }
  }

  private void queryString(String queryString, Consumer<QueryString> consumer) {
    consumer.accept(UrlParser.queryString(ValueConverterHelper.testContext(), queryString));
  }
}
