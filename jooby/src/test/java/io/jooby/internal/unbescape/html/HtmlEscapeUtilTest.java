/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HtmlEscapeUtilTest {

  private HtmlEscapeSymbols symbols;
  private byte[] originalEscapeLevels;
  private Map<Integer, Short> originalOverflow;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    // 1. Retrieve the static HTML5_SYMBOLS instance
    Field symbolsField = HtmlEscapeSymbols.class.getDeclaredField("HTML5_SYMBOLS");
    symbolsField.setAccessible(true);
    symbols = (HtmlEscapeSymbols) symbolsField.get(null);

    // 2. Backup and manipulate ESCAPE_LEVELS for deterministic testing
    originalEscapeLevels = symbols.ESCAPE_LEVELS.clone();

    // We arbitrarily set 'a' to level 5, and '<' to level 1.
    // This allows us to pass level 2 and guarantee 'a' is skipped while '<' is escaped.
    symbols.ESCAPE_LEVELS['a'] = 5;
    symbols.ESCAPE_LEVELS['<'] = 1;
    // Set non-ASCII fallback to 5
    symbols.ESCAPE_LEVELS[HtmlEscapeSymbols.MAX_ASCII_CHAR + 1] = 5;

    // 3. Backup and manipulate NCRS_BY_CODEPOINT_OVERFLOW map
    Field overflowField = HtmlEscapeSymbols.class.getDeclaredField("NCRS_BY_CODEPOINT_OVERFLOW");
    overflowField.setAccessible(true);
    originalOverflow = (Map<Integer, Short>) overflowField.get(symbols);

    // Inject an entry for our Surrogate Pair test (Codepoint 128512 = 😀) pointing to NCR index 0
    if (symbols.NCRS_BY_CODEPOINT_OVERFLOW != null) {
      symbols.NCRS_BY_CODEPOINT_OVERFLOW.put(128512, (short) 0);
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    // Restore ESCAPE_LEVELS
    System.arraycopy(
        originalEscapeLevels, 0, symbols.ESCAPE_LEVELS, 0, originalEscapeLevels.length);

    // Restore OVERFLOW map completely using the saved reference
    Field overflowField = HtmlEscapeSymbols.class.getDeclaredField("NCRS_BY_CODEPOINT_OVERFLOW");
    overflowField.setAccessible(true);
    overflowField.set(symbols, originalOverflow);
  }

  // Helper method to mock HtmlEscapeType
  private HtmlEscapeType mockType(boolean useNCRs, boolean useHexa) {
    HtmlEscapeType type = mock(HtmlEscapeType.class);
    when(type.getUseNCRs()).thenReturn(useNCRs);
    when(type.getUseHexa()).thenReturn(useHexa);
    return type;
  }

  // Helper method to mock HtmlEscapeLevel
  private HtmlEscapeLevel mockLevel(int level) {
    HtmlEscapeLevel escapeLevel = mock(HtmlEscapeLevel.class);
    when(escapeLevel.getEscapeLevel()).thenReturn(level);
    return escapeLevel;
  }

  @Test
  @DisplayName("Verify null input returns null fast exit")
  void testNullInput() {
    assertNull(HtmlEscapeUtil.escape(null, mockType(true, true), mockLevel(0)));
  }

  @Test
  @DisplayName("Verify fast exit when no escape is needed for ASCII and Non-ASCII")
  void testNoEscapeNeeded() {
    // level = 0.
    // 'a' level is 5. 0 < 5 -> true -> skip.
    // 'á' (non-ascii) level is 5. 0 < 5 -> true -> skip.
    String input = "a\u00E1a";

    String result = HtmlEscapeUtil.escape(input, mockType(true, true), mockLevel(0));
    assertEquals(input, result);
  }

  @Test
  @DisplayName("Verify ASCII escape using NCR and unescaped tail appending")
  void testEscapeASCII_NCR() {
    // level = 2.
    // 'a' level is 5. 2 < 5 -> true -> skip.
    // '<' level is 1. 2 < 1 -> false -> escape.
    String input = "a<a";

    // Html5 defines '<' as "&lt;"
    String result = HtmlEscapeUtil.escape(input, mockType(true, false), mockLevel(2));
    assertEquals("a&lt;a", result);
  }

  @Test
  @DisplayName("Verify ASCII escape falling back to Hexa DCR when NO NCR exists")
  void testEscapeASCII_NoNCR_Hexa() {
    // \u0001 is an unprintable control char with no predefined NCR
    symbols.ESCAPE_LEVELS['\u0001'] = 1; // Force escape

    String input = "\u0001";
    String result = HtmlEscapeUtil.escape(input, mockType(true, true), mockLevel(2));

    assertEquals("&#x1;", result);
  }

  @Test
  @DisplayName("Verify ASCII escape falling back to Decimal DCR when NO NCR exists")
  void testEscapeASCII_NoNCR_Decimal() {
    symbols.ESCAPE_LEVELS['\u0001'] = 1; // Force escape

    String input = "\u0001";
    String result = HtmlEscapeUtil.escape(input, mockType(true, false), mockLevel(2));

    assertEquals("&#1;", result);
  }

  @Test
  @DisplayName("Verify Surrogate Pair codepoint escaping using the Overflow map")
  void testEscapeSurrogatePair_WithNCR() {
    // \uD83D\uDE00 is 😀 (Codepoint 128512). We forcibly mapped this to NCR index 0 in setUp.
    String input = "\uD83D\uDE00";

    // Dynamically retrieve what index 0 holds in the real HTML5_SYMBOLS
    String expectedNcr = new String(symbols.SORTED_NCRS[0]);

    String result = HtmlEscapeUtil.escape(input, mockType(true, false), mockLevel(10));
    assertEquals(expectedNcr, result);
  }

  @Test
  @DisplayName("Verify Surrogate Pair codepoint falling back to Hexa DCR when absent from Overflow")
  void testEscapeSurrogatePair_NoNCR_Hexa() {
    // \uD83D\uDE01 is 😁 (Codepoint 128513). This is NOT in our overflow map.
    String input = "\uD83D\uDE01";

    String result = HtmlEscapeUtil.escape(input, mockType(true, true), mockLevel(10));
    assertEquals("&#x1f601;", result);
  }

  @Test
  @DisplayName("Verify bypassing NCR logic entirely when useNCRs = false")
  void testEscapeUseNCRsFalse() {
    // ASCII '<' which has an NCR, but we pass useNCRs = false
    String input = "<";

    String result = HtmlEscapeUtil.escape(input, mockType(false, false), mockLevel(2));
    assertEquals("&#60;", result); // Falls directly to decimal
  }

  @Test
  @DisplayName("Verify null check branch for the Overflow Map")
  void testOverflowMapNullBranch() throws Exception {
    Field overflowField = HtmlEscapeSymbols.class.getDeclaredField("NCRS_BY_CODEPOINT_OVERFLOW");
    overflowField.setAccessible(true);

    // Reflection allows us to modify the final instance field to null temporarily
    @SuppressWarnings("unchecked")
    Map<Integer, Short> original = (Map<Integer, Short>) overflowField.get(symbols);

    try {
      overflowField.set(symbols, null);

      // Trigger a surrogate pair so the code tries to check the overflow map
      String input = "\uD83D\uDE01";

      String result = HtmlEscapeUtil.escape(input, mockType(true, false), mockLevel(10));
      assertEquals("&#128513;", result); // Successfully falls back to Decimal since map is null

    } finally {
      overflowField.set(symbols, original);
    }
  }

  @Test
  @DisplayName("Verify instantiation of private utility constructor")
  void testPrivateConstructor() throws Exception {
    Constructor<HtmlEscapeUtil> constructor = HtmlEscapeUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    HtmlEscapeUtil instance = constructor.newInstance();
    assertNotNull(instance);
  }
}
