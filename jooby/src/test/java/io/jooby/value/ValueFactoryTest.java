/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Reified;
import io.jooby.StatusCode;
import io.jooby.exception.TypeMismatchException;

public class ValueFactoryTest {

  private ValueFactory factory;

  @BeforeEach
  void setUp() {
    factory = new ValueFactory();
  }

  @Test
  @DisplayName("Test ValueFactory basic configuration and hints")
  void testConfig() {
    factory.hint(ConversionHint.Nullable);
    // Missing value with Nullable hint should return null
    assertNull(factory.convert(String.class, Value.missing(factory, "m")));

    factory.hint(ConversionHint.Strict);
    // Missing value with Strict hint should throw TypeMismatchException
    assertThrows(
        TypeMismatchException.class,
        () -> factory.convert(String.class, Value.missing(factory, "m")));

    // Test explicit lookup set
    factory.lookup(MethodHandles.publicLookup());
    assertNotNull(factory.get(String.class));
  }

  @Test
  @DisplayName("Test Container conversions (List, Set, Optional)")
  void testContainers() {
    Value val = Value.value(factory, "n", "123");

    List<Integer> list = factory.convert(Reified.list(Integer.class).getType(), val);
    assertEquals(List.of(123), list);

    Set<Integer> set = factory.convert(Reified.set(Integer.class).getType(), val);
    assertEquals(Set.of(123), set);

    Optional<Integer> opt = factory.convert(Reified.optional(Integer.class).getType(), val);
    assertEquals(Optional.of(123), opt);
  }

  @Test
  @DisplayName("Test Enum resolution branches")
  void testEnums() {
    Value val = Value.value(factory, "e", "a");
    // Case-insensitive match via EnumSet loop in ValueFactory.enumValue
    assertEquals(
        Thread.State.NEW, factory.convert(Thread.State.class, Value.value(factory, "e", "new")));

    // Exact match (toUpperCase)
    assertEquals(
        Thread.State.RUNNABLE,
        factory.convert(Thread.State.class, Value.value(factory, "e", "RUNNABLE")));

    // Invalid enum
    assertThrows(
        IllegalArgumentException.class,
        () -> factory.convert(Thread.State.class, Value.value(factory, "e", "NOT_EXIST")));
  }

  @Test
  @DisplayName("Test dynamic valueOf and Constructor fallbacks")
  void testDynamicResolution() {
    // valueOf(String) branch (Integer has valueOf)
    assertEquals(
        Integer.valueOf(42), factory.convert(Integer.class, Value.value(factory, "v", "42")));

    // Constructor(String) branch
    // UUID.fromString is static, but UUID(String) doesn't exist. Let's use a class that has
    // constructor(String).
    // java.io.File has constructor(String)
    java.io.File file =
        factory.convert(java.io.File.class, Value.value(factory, "path", "temp.txt"));
    assertEquals("temp.txt", file.getName());
  }

  @Test
  @DisplayName("Test StandardConverter: Numeric and Primitives")
  void testStandardNumeric() {
    Value val = Value.value(factory, "v", "1");
    Value missing = Value.missing(factory, "m");

    var intValue = factory.convert(int.class, val);
    assertEquals(1, intValue);
    assertEquals(Integer.valueOf(1), factory.convert(Integer.class, val));
    assertNull(StandardConverter.Int.convert(Integer.class, missing, ConversionHint.Nullable));

    var longVal = factory.convert(long.class, val);
    assertEquals(longVal, factory.convert(long.class, val));
    assertEquals(Long.valueOf(1), factory.convert(Long.class, val));
    assertNull(StandardConverter.Long.convert(Long.class, missing, ConversionHint.Nullable));

    var floatVal = factory.convert(float.class, val);
    assertEquals(floatVal, factory.convert(float.class, val));
    assertEquals(Float.valueOf(1), factory.convert(Float.class, val));
    assertNull(StandardConverter.Float.convert(Float.class, missing, ConversionHint.Nullable));

    assertEquals(1.0d, factory.convert(double.class, val));
    assertEquals(1.0d, factory.convert(Double.class, val));
    assertNull(StandardConverter.Double.convert(Double.class, missing, ConversionHint.Nullable));

    var byteVal = factory.convert(byte.class, val);
    assertEquals(byteVal, factory.convert(byte.class, val));
    assertEquals(Byte.valueOf((byte) 1), factory.convert(Byte.class, val));
    assertNull(StandardConverter.Byte.convert(Byte.class, missing, ConversionHint.Nullable));

    assertTrue((Boolean) factory.convert(boolean.class, Value.value(factory, "v", "true")));
    assertTrue((Boolean) factory.convert(Boolean.class, Value.value(factory, "v", "true")));
    assertNull(StandardConverter.Boolean.convert(Boolean.class, missing, ConversionHint.Nullable));

    assertEquals(new BigInteger("1"), factory.convert(BigInteger.class, val));
    assertEquals(new BigDecimal("1"), factory.convert(BigDecimal.class, val));
  }

  @Test
  @DisplayName("Test StandardConverter: Charset")
  void testCharset() {
    assertEquals(
        StandardCharsets.UTF_8, factory.convert(Charset.class, Value.value(factory, "c", "UTF-8")));
    assertEquals(
        StandardCharsets.US_ASCII,
        factory.convert(Charset.class, Value.value(factory, "c", "US-ASCII")));
    assertEquals(
        StandardCharsets.ISO_8859_1,
        factory.convert(Charset.class, Value.value(factory, "c", "ISO-8859-1")));
    assertEquals(
        StandardCharsets.UTF_16,
        factory.convert(Charset.class, Value.value(factory, "c", "UTF-16")));
    assertEquals(
        StandardCharsets.UTF_16BE,
        factory.convert(Charset.class, Value.value(factory, "c", "UTF-16BE")));
    assertEquals(
        StandardCharsets.UTF_16LE,
        factory.convert(Charset.class, Value.value(factory, "c", "UTF-16LE")));
    // Fallback
    assertEquals(
        Charset.forName("GBK"), factory.convert(Charset.class, Value.value(factory, "c", "GBK")));
  }

  @Test
  @DisplayName("Test StandardConverter: Date and Time")
  void testDateTime() {
    long now = System.currentTimeMillis();
    String nowStr = String.valueOf(now);
    String isoDate = "2023-01-01";
    String isoDateTime = "2023-01-01T10:00:00";

    // Date: Millis vs ISO
    assertEquals(new Date(now), factory.convert(Date.class, Value.value(factory, "d", nowStr)));
    assertNotNull(factory.convert(Date.class, Value.value(factory, "d", isoDate)));

    // Instant: Millis vs ISO
    assertEquals(
        Instant.ofEpochMilli(now),
        factory.convert(Instant.class, Value.value(factory, "i", nowStr)));
    assertEquals(
        Instant.parse("2023-01-01T10:00:00Z"),
        factory.convert(Instant.class, Value.value(factory, "i", "2023-01-01T10:00:00Z")));

    // LocalDate: Millis vs ISO
    assertNotNull(factory.convert(LocalDate.class, Value.value(factory, "ld", nowStr)));
    assertEquals(
        LocalDate.of(2023, 1, 1),
        factory.convert(LocalDate.class, Value.value(factory, "ld", isoDate)));

    // LocalDateTime: Millis vs ISO
    assertNotNull(factory.convert(LocalDateTime.class, Value.value(factory, "ldt", nowStr)));
    assertEquals(
        LocalDateTime.of(2023, 1, 1, 10, 0, 0),
        factory.convert(LocalDateTime.class, Value.value(factory, "ldt", isoDateTime)));
  }

  @Test
  @DisplayName("Test StandardConverter: Duration and Period")
  void testDurationAndPeriod() {
    // Duration ISO vs Units
    assertEquals(
        Duration.ofMinutes(5), factory.convert(Duration.class, Value.value(factory, "d", "PT5M")));
    assertEquals(
        Duration.ofMillis(500),
        factory.convert(Duration.class, Value.value(factory, "d", "500ms")));
    assertEquals(
        Duration.ofSeconds(10), factory.convert(Duration.class, Value.value(factory, "d", "10s")));
    assertEquals(
        Duration.ofHours(1), factory.convert(Duration.class, Value.value(factory, "d", "1h")));
    assertEquals(
        Duration.ofDays(2), factory.convert(Duration.class, Value.value(factory, "d", "2d")));
    assertEquals(
        Duration.ofNanos(100), factory.convert(Duration.class, Value.value(factory, "d", "100ns")));
    assertEquals(
        Duration.ofNanos(1000), factory.convert(Duration.class, Value.value(factory, "d", "1us")));

    // Duration errors
    assertThrows(
        Exception.class,
        () -> factory.convert(Duration.class, Value.value(factory, "d", "ms"))); // No number
    assertThrows(
        Exception.class,
        () -> factory.convert(Duration.class, Value.value(factory, "d", "10x"))); // Bad unit

    // Period
    assertEquals(Period.ofDays(1), factory.convert(Period.class, Value.value(factory, "p", "1d")));
    assertEquals(Period.ofWeeks(2), factory.convert(Period.class, Value.value(factory, "p", "2w")));
    assertEquals(
        Period.ofMonths(3), factory.convert(Period.class, Value.value(factory, "p", "3m")));
    assertEquals(Period.ofYears(1), factory.convert(Period.class, Value.value(factory, "p", "1y")));
    // Period errors
    assertThrows(
        Exception.class,
        () -> factory.convert(Period.class, Value.value(factory, "p", "1h"))); // Time-based
  }

  @Test
  @DisplayName("Test StandardConverter: URI, URL, UUID, Zone")
  void testMisc() throws Exception {
    assertEquals(
        new URI("http://jooby.io"),
        factory.convert(URI.class, Value.value(factory, "u", "http://jooby.io")));
    assertEquals(
        new URL("http://jooby.io"),
        factory.convert(URL.class, Value.value(factory, "u", "http://jooby.io")));

    UUID uuid = UUID.randomUUID();
    assertEquals(uuid, factory.convert(UUID.class, Value.value(factory, "id", uuid.toString())));

    assertEquals(ZoneId.of("UTC"), factory.convert(ZoneId.class, Value.value(factory, "z", "UTC")));
    assertEquals(
        TimeZone.getTimeZone("UTC"),
        factory.convert(TimeZone.class, Value.value(factory, "z", "UTC")));

    assertEquals(
        StatusCode.OK, factory.convert(StatusCode.class, Value.value(factory, "s", "200")));
  }
}
