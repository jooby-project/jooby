/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.QueryString;
import io.jooby.value.ValueFactory;

public class UrlParserTest {

  @Test
  @DisplayName("Instantiate utility class for complete coverage")
  public void testConstructor() {
    assertNotNull(new UrlParser());
  }

  @Test
  @DisplayName("Test QueryString with null or empty inputs")
  public void testQueryStringNullOrEmpty() {
    ValueFactory factory = new ValueFactory();

    QueryString qsNull = UrlParser.queryString(factory, null);
    assertEquals(0, qsNull.toMap().size());

    QueryString qsEmpty = UrlParser.queryString(factory, "");
    assertEquals(0, qsEmpty.toMap().size());
  }

  @Test
  @DisplayName("Test Path Segment Decoding")
  public void testDecodePathSegment() {
    assertEquals("", UrlParser.decodePathSegment(null));
    assertEquals("", UrlParser.decodePathSegment(""));

    // In paths, '+' is not decoded as space
    assertEquals("a+b c", UrlParser.decodePathSegment("a+b%20c"));
    assertEquals("a/b", UrlParser.decodePathSegment("a%2Fb"));
  }

  @Test
  @DisplayName("Test Standard URL Query Parameter Parsing")
  public void testDecodeParamsNormal() {
    ValueFactory factory = new ValueFactory();

    // Normal params with mixed separators and a fragment
    QueryString qs = UrlParser.queryString(factory, "?a=1&b=2;c=3#fragment");
    Map<String, String> map = qs.toMap();

    assertEquals("1", map.get("a"));
    assertEquals("2", map.get("b"));
    assertEquals("3", map.get("c"));
    assertFalse(map.containsKey("fragment")); // Fragment correctly ignored
  }

  @Test
  @DisplayName("Test Query String edge cases and branch anomalies")
  public void testDecodeParamsEdgeCases() {
    ValueFactory factory = new ValueFactory();

    // Param with no value
    QueryString qs1 = UrlParser.queryString(factory, "?flag");
    assertEquals("", qs1.get("flag").value());

    // Empty param name ("?=val" -> name becomes "val", value becomes "")
    QueryString qs2 = UrlParser.queryString(factory, "?=val");
    assertEquals("", qs2.get("val").value());

    // Multiple equals
    QueryString qs3 = UrlParser.queryString(factory, "?a=b=c");
    assertEquals("b=c", qs3.get("a").value());

    // Trailing '&' separator
    QueryString qs4 = UrlParser.queryString(factory, "?a=1&");
    assertEquals("1", qs4.get("a").value());
    assertEquals(1, qs4.toMap().size());

    // Only '?'
    QueryString qs5 = UrlParser.queryString(factory, "?");
    assertEquals(0, qs5.toMap().size());

    // No '?' at the start
    QueryString qs6 = UrlParser.queryString(factory, "a=1");
    assertEquals("1", qs6.get("a").value());
  }

  @Test
  @DisplayName("Test Query Component decoding (Spaces and Pluses)")
  public void testDecodeComponentQuerySpaces() {
    ValueFactory factory = new ValueFactory();

    // In query strings, '+' should be decoded as space
    QueryString qs = UrlParser.queryString(factory, "a+b=c+d");
    assertEquals("c d", qs.get("a b").value());
  }

  @Test
  @DisplayName("Test Hex decoding and character conversion")
  public void testDecodeComponentHexParsing() {
    ValueFactory factory = new ValueFactory();

    // Sequential % sequences
    QueryString qs1 = UrlParser.queryString(factory, "a=%30%41%61");
    assertEquals("0Aa", qs1.get("a").value());

    // Regular characters following a % sequence
    QueryString qs2 = UrlParser.queryString(factory, "a=%30x");
    assertEquals("0x", qs2.get("a").value());
  }

  @Test
  @DisplayName("Test Malformed Hex and Character Coding Exceptions")
  public void testDecodeComponentExceptions() {
    ValueFactory factory = new ValueFactory();

    // Unterminated sequence
    IllegalArgumentException ex1 =
        assertThrows(IllegalArgumentException.class, () -> UrlParser.queryString(factory, "a=%2"));
    assertTrue(ex1.getMessage().contains("unterminated escape sequence"));

    // Invalid hex byte (high bit)
    IllegalArgumentException ex2 =
        assertThrows(IllegalArgumentException.class, () -> UrlParser.queryString(factory, "a=%Z1"));
    assertTrue(ex2.getMessage().contains("invalid hex byte"));

    // Invalid hex byte (low bit)
    assertThrows(IllegalArgumentException.class, () -> UrlParser.queryString(factory, "a=%1Z"));

    // Invalid Hex Nibble boundary conditions (forces all branches in decodeHexNibble)
    assertThrows(
        IllegalArgumentException.class, () -> UrlParser.queryString(factory, "a=%/1")); // Below '0'
    assertThrows(
        IllegalArgumentException.class,
        () -> UrlParser.queryString(factory, "a=%:1")); // Between '9' and 'A'
    assertThrows(
        IllegalArgumentException.class,
        () -> UrlParser.queryString(factory, "a=%[1")); // Between 'F' and 'a'
    assertThrows(
        IllegalArgumentException.class, () -> UrlParser.queryString(factory, "a=%g1")); // Above 'f'

    // Malformed input (fails on decoder.decode with isUnderflow == false)
    assertThrows(Exception.class, () -> UrlParser.queryString(factory, "a=%FF"));

    // Malformed input (incomplete multi-byte sequence, fails on decoder.flush)
    assertThrows(Exception.class, () -> UrlParser.queryString(factory, "a=%C3"));
  }

  @Test
  @DisplayName("Test parameter extraction limit (Hardcapped at 1024)")
  public void testParamsLimit() {
    ValueFactory factory = new ValueFactory();
    StringBuilder sb = new StringBuilder();

    // Generate string with 1030 parameters
    for (int i = 0; i < 1030; i++) {
      sb.append("k").append(i).append("=v&");
    }

    QueryString qs = UrlParser.queryString(factory, sb.toString());

    // Verifies the `if (paramsLimit == 0) { return; }` branch executed
    assertEquals(1024, qs.toMap().size());
  }

  @Test
  @DisplayName("Test decodeParams early return boundary")
  public void testPrivateDecodeParamsEarlyReturn() throws Exception {

    // If the early return didn't execute, it would throw an NPE trying to access `null` root
    // HashValue.
    assertDoesNotThrow(() -> UrlParser.decodeParams(null, "abc", 5, StandardCharsets.UTF_8, 10));
  }
}
