/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import static org.junit.jupiter.api.Assertions.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import io.jooby.QueryString;
import io.jooby.exception.TypeMismatchException;
import io.jooby.internal.UrlParser;

public class ValueTest {

  private final ValueFactory factory = new ValueFactory();

  @Test
  public void simpleQueryString() {
    queryString(
        "&foo=bar",
        qs -> {
          assertEquals("?&foo=bar", qs.queryString());
          assertEquals("bar", qs.get("foo").value());
          assertEquals(1, qs.size());
        });
    queryString(
        "foo=bar&",
        qs -> {
          assertEquals("?foo=bar&", qs.queryString());
          assertEquals("bar", qs.get("foo").value());
        });
    queryString(
        "a=1&b=2",
        qs -> {
          assertEquals(1, qs.get("a").intValue());
          assertEquals(2, qs.get("b").intValue());
        });
    queryString(
        "a=1&a=2",
        qs -> {
          assertEquals(1, qs.get("a").get(0).intValue());
          assertEquals(2, qs.get("a").get(1).intValue());
        });
    queryString("", qs -> assertEquals(0, qs.size()));
    queryString(null, qs -> assertEquals(0, qs.size()));
  }

  @Test
  @DisplayName("Test Variable Resolution (resolve method)")
  public void resolve() {
    Value root =
        Value.hash(
            factory,
            Map.of(
                "user", List.of("root"),
                "db", List.of("prod"),
                "port", List.of("8080")));

    // Happy path
    assertEquals("prod", root.resolve("${db}"));
    assertEquals("User: root, Port: 8080", root.resolve("User: ${user}, Port: ${port}"));

    // Custom delimiters
    assertEquals("prod", root.resolve("<%db%>", "<%", "%>"));

    // Dot notation resolution
    Value nested = Value.hash(factory, Map.of("app.env", List.of("dev")));
    assertEquals("dev", nested.resolve("${app.env}"));

    // ignoreMissing = true
    assertEquals("Hello ${missing}", root.resolve("Hello ${missing}", true));

    // Empty expression branch
    assertEquals("", root.resolve(""));

    // No placeholders branch (returns original)
    assertEquals("plain text", root.resolve("plain text"));

    // Error: Unclosed delimiter
    assertThrows(IllegalArgumentException.class, () -> root.resolve("Hello ${world"));

    // Error: Missing key (ignoreMissing = false)
    assertThrows(NoSuchElementException.class, () -> root.resolve("${missing}"));
  }

  @Test
  @DisplayName("Test numeric conversions and Date-to-Long")
  public void numericConversions() {
    Value val = Value.value(factory, "n", "123");
    assertEquals(123L, val.longValue());
    assertEquals(123, val.intValue());
    assertEquals((byte) 123, val.byteValue());
    assertEquals(123.0f, val.floatValue());
    assertEquals(123.0d, val.doubleValue());

    // Fallbacks
    Value missing = Value.missing(factory, "m");
    assertEquals(9L, missing.longValue(9L));
    assertEquals(9, missing.intValue(9));
    assertEquals((byte) 9, missing.byteValue((byte) 9));
    assertEquals(9.0f, missing.floatValue(9.0f));
    assertEquals(9.0d, missing.doubleValue(9.0d));

    // Date parsing in longValue
    String dateStr = "Wed, 21 Oct 2015 07:28:00 GMT";
    Value dateVal = Value.value(factory, "date", dateStr);
    long expectedMillis =
        java.time.ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant()
            .toEpochMilli();
    assertEquals(expectedMillis, dateVal.longValue());

    // Type Mismatch
    assertThrows(
        TypeMismatchException.class, () -> Value.value(factory, "x", "not-a-number").longValue());
    assertThrows(
        TypeMismatchException.class, () -> Value.value(factory, "x", "not-a-number").intValue());
  }

  @Test
  @DisplayName("Test Boolean and String conversions")
  public void otherConversions() {
    assertTrue(Value.value(factory, "b", "true").booleanValue());
    assertFalse(Value.value(factory, "b", "false").booleanValue());
    assertTrue(Value.missing(factory, "m").booleanValue(true));

    assertEquals("fallback", Value.missing(factory, "m").value("fallback"));
    assertNull(Value.missing(factory, "m").valueOrNull());
    assertEquals("val", Value.value(factory, "v", "val").valueOrNull());
  }

  @Test
  @DisplayName("Test Type checks (isPresent, isArray, etc.)")
  public void typeChecks() {
    Value single = Value.value(factory, "s", "v");
    assertTrue(single.isSingle());
    assertTrue(single.isPresent());
    assertFalse(single.isMissing());
    assertFalse(single.isArray());
    assertFalse(single.isObject());

    Value missing = Value.missing(factory, "m");
    assertTrue(missing.isMissing());
    assertFalse(missing.isPresent());

    Value array = Value.array(factory, "a", List.of("1", "2"));
    assertTrue(array.isArray());

    Value hash = Value.hash(factory, Map.of("k", List.of("v")));
    assertTrue(hash.isObject());
  }

  @Test
  @DisplayName("Test Static Factory Methods (create, headers, formdata)")
  public void staticFactories() {
    // create(List)
    assertTrue(Value.create(factory, "x", (List<String>) null).isMissing());
    assertTrue(Value.create(factory, "x", List.of()).isMissing());
    assertTrue(Value.create(factory, "x", List.of("1")).isSingle());
    assertTrue(Value.create(factory, "x", List.of("1", "2")).isArray());

    // create(String)
    assertTrue(Value.create(factory, "x", (String) null).isMissing());
    assertTrue(Value.create(factory, "x", "val").isSingle());

    // headers & formdata
    assertNotNull(Value.headers(factory, Map.of("h", List.of("v"))));
    assertNotNull(Value.formdata(factory));
  }

  @Test
  @DisplayName("Test Collections and Maps")
  public void collections() {
    Value val = Value.value(factory, "n", "123");
    assertEquals(Optional.of("123"), val.toOptional());
    assertEquals(Optional.empty(), Value.missing(factory, "m").toOptional());

    // Typed collections (Note: These often delegate to internal 'to' logic)
    assertNotNull(val.toList(String.class));
    assertNotNull(val.toSet(String.class));
    assertNotNull(val.toOptional(String.class));

    // Maps
    queryString(
        "a=1&b=2",
        qs -> {
          Map<String, String> map = qs.toMap();
          assertEquals("1", map.get("a"));
          assertEquals("2", map.get("b"));
        });
  }

  @Test
  public void toEnum() {
    Value val = Value.value(factory, "e", "a");
    assertEquals(Letter.A, val.toEnum(Letter::valueOf));
    // custom name provider (lowercase to uppercase)
    assertEquals(
        Letter.B, Value.value(factory, "e", "b").toEnum(Letter::valueOf, String::toUpperCase));
  }

  enum Letter {
    A,
    B
  }

  public static <T extends Throwable> void assertMessage(
      Class<T> expectedType, Executable executable, String message) {
    T x = assertThrows(expectedType, executable);
    if (message != null) {
      assertEquals(message, x.getMessage());
    }
  }

  private void queryString(String queryString, Consumer<QueryString> consumer) {
    consumer.accept(UrlParser.queryString(factory, queryString));
  }
}
