/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.json;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JsonEscapeUtilTest {

  // Helper method to mock JsonEscapeType
  private JsonEscapeType mockType(boolean useSECs) {
    JsonEscapeType type = mock(JsonEscapeType.class);
    when(type.getUseSECs()).thenReturn(useSECs);
    return type;
  }

  // Helper method to mock JsonEscapeLevel
  private JsonEscapeLevel mockLevel(int level) {
    JsonEscapeLevel escapeLevel = mock(JsonEscapeLevel.class);
    when(escapeLevel.getEscapeLevel()).thenReturn(level);
    return escapeLevel;
  }

  @Test
  @DisplayName("Verify toUHexa correctly converts integers to 4-character hex arrays")
  void testToUHexa() {
    assertArrayEquals(new char[] {'0', '0', '0', '0'}, JsonEscapeUtil.toUHexa(0));
    assertArrayEquals(new char[] {'1', '2', '3', '4'}, JsonEscapeUtil.toUHexa(0x1234));
    assertArrayEquals(new char[] {'A', 'B', 'C', 'D'}, JsonEscapeUtil.toUHexa(0xABCD));
    assertArrayEquals(new char[] {'F', 'F', 'F', 'F'}, JsonEscapeUtil.toUHexa(0xFFFF));
  }

  @Test
  @DisplayName("Verify fast exit when no escape is required for standard ASCII text")
  void testNoEscapeNeeded() {
    String input = "abc 123";
    // Level 1 skips standard alphanumeric/safe chars
    String result = JsonEscapeUtil.escape(input, mockType(true), mockLevel(1));
    assertEquals(input, result);
  }

  @Test
  @DisplayName("Verify Solidus (slash) specific escape rules and context awareness")
  void testSolidusEscapeRules() {
    // 1. Slash at offset 0 (skipped if level < 3)
    assertEquals("/", JsonEscapeUtil.escape("/", mockType(true), mockLevel(2)));

    // 2. Slash preceded by non-'<' (skipped if level < 3)
    assertEquals("a/", JsonEscapeUtil.escape("a/", mockType(true), mockLevel(2)));

    // 3. Slash preceded by '<' (always escaped to prevent </script> issues)
    // Note: '<' is level 3. At level 2, '<' is skipped, but '/' is escaped!
    assertEquals("<\\/", JsonEscapeUtil.escape("</", mockType(true), mockLevel(2)));

    // 4. Slash with Level >= 3 (always escaped regardless of preceding char)
    assertEquals("\\/", JsonEscapeUtil.escape("/", mockType(true), mockLevel(3)));
  }

  @Test
  @DisplayName("Verify Single Escape Characters (SECs) correctly map to backslash shortcuts")
  void testSECs() {
    String input = "\b\t\n\f\r\"\\";
    String expected = "\\b\\t\\n\\f\\r\\\"\\\\";

    String result = JsonEscapeUtil.escape(input, mockType(true), mockLevel(2));
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Verify UHEXA fallback when UseSECs is false")
  void testUseSECsFalse() {
    String input = "\n";
    // SEC would be \n, but disabled -> falls back to
    String result = JsonEscapeUtil.escape(input, mockType(false), mockLevel(2));
    assertEquals("\\u000A", result);
  }

  @Test
  @DisplayName("Verify UHEXA mapping for control characters missing SEC shortcuts")
  void testControlCharHexa() {
    String input = "\u0001";
    // \u0001 is forced to escape at Level 1, but has no SEC shortcut defined.
    String result = JsonEscapeUtil.escape(input, mockType(true), mockLevel(1));
    assertEquals("\\u0001", result);
  }

  @Test
  @DisplayName("Verify skipping of single and surrogate Non-ASCII characters based on level")
  void testSkipNonAscii() {
    // Non-ASCII fallback level in JsonEscapeUtil is 2.
    // If we request Level 1, these should be safely skipped.

    // Single-char Non-ASCII (á = \u00E1)
    assertEquals("\u00E1", JsonEscapeUtil.escape("\u00E1", mockType(true), mockLevel(1)));

    // Surrogate pair Non-ASCII (😀 = \uD83D\uDE00)
    assertEquals("😀", JsonEscapeUtil.escape("😀", mockType(true), mockLevel(1)));
  }

  @Test
  @DisplayName("Verify escaping of single and surrogate Non-ASCII characters based on level")
  void testEscapeNonAscii() {
    // Level 2 forces Non-ASCII to be escaped.

    // Single-char Non-ASCII
    assertEquals("\\u00E1", JsonEscapeUtil.escape("\u00E1", mockType(true), mockLevel(2)));

    // Surrogate pair Non-ASCII (High Surrogate + Low Surrogate)
    assertEquals("\\uD83D\\uDE00", JsonEscapeUtil.escape("😀", mockType(true), mockLevel(2)));
  }

  @Test
  @DisplayName("Verify correct text slicing and appending for mixed escaped/unescaped strings")
  void testMixedStringSlicing() {
    // Escaped content at the beginning
    assertEquals("\\nabc", JsonEscapeUtil.escape("\nabc", mockType(true), mockLevel(1)));

    // Escaped content in the middle
    assertEquals("a\\nb", JsonEscapeUtil.escape("a\nb", mockType(true), mockLevel(1)));

    // Escaped content at the end
    assertEquals("abc\\n", JsonEscapeUtil.escape("abc\n", mockType(true), mockLevel(1)));
  }

  @Test
  @DisplayName("Verify instantiation of private utility constructor")
  void testPrivateConstructor() throws Exception {
    Constructor<JsonEscapeUtil> constructor = JsonEscapeUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    JsonEscapeUtil instance = constructor.newInstance();
    assertNotNull(instance);
  }
}
