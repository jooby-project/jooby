/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.uri;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.internal.unbescape.uri.UriEscapeUtil.UriEscapeType;

public class UriEscapeUtilTest {

  @Test
  @DisplayName("Verify synthetic enum methods for UriEscapeType")
  void testEnumSyntheticMethods() {
    UriEscapeType[] values = UriEscapeType.values();
    assertEquals(4, values.length);
    assertEquals(UriEscapeType.PATH, UriEscapeType.valueOf("PATH"));
  }

  @Test
  @DisplayName("Verify PATH escape type allowed characters")
  void testPathEscapeType() {
    assertTrue(UriEscapeType.PATH.isAllowed('a'));
    assertTrue(UriEscapeType.PATH.isAllowed('/'));
    assertFalse(UriEscapeType.PATH.isAllowed('?'));
    assertFalse(UriEscapeType.PATH.canPlusEscapeWhitespace());
  }

  @Test
  @DisplayName("Verify PATH_SEGMENT escape type allowed characters")
  void testPathSegmentEscapeType() {
    assertTrue(UriEscapeType.PATH_SEGMENT.isAllowed('a'));
    assertFalse(UriEscapeType.PATH_SEGMENT.isAllowed('/'));
    assertFalse(UriEscapeType.PATH_SEGMENT.canPlusEscapeWhitespace());
  }

  @Test
  @DisplayName("Verify QUERY_PARAM escape type allowed characters")
  void testQueryParamEscapeType() {
    assertTrue(UriEscapeType.QUERY_PARAM.isAllowed('a'));
    assertTrue(UriEscapeType.QUERY_PARAM.isAllowed('/'));
    assertTrue(UriEscapeType.QUERY_PARAM.isAllowed('?'));

    // Explicit exclusions for QUERY_PARAM
    assertFalse(UriEscapeType.QUERY_PARAM.isAllowed('='));
    assertFalse(UriEscapeType.QUERY_PARAM.isAllowed('&'));
    assertFalse(UriEscapeType.QUERY_PARAM.isAllowed('+'));
    assertFalse(UriEscapeType.QUERY_PARAM.isAllowed('#'));

    assertTrue(UriEscapeType.QUERY_PARAM.canPlusEscapeWhitespace());
  }

  @Test
  @DisplayName("Verify FRAGMENT_ID escape type allowed characters")
  void testFragmentIdEscapeType() {
    assertTrue(UriEscapeType.FRAGMENT_ID.isAllowed('a'));
    assertTrue(UriEscapeType.FRAGMENT_ID.isAllowed('/'));
    assertTrue(UriEscapeType.FRAGMENT_ID.isAllowed('?'));
    assertFalse(UriEscapeType.FRAGMENT_ID.isAllowed('#'));
    assertFalse(UriEscapeType.FRAGMENT_ID.canPlusEscapeWhitespace());
  }

  @Test
  @DisplayName("Verify all unreserved, digit, and alpha branches")
  void testUnreservedAndAlphaBranches() {
    // Tests isAlpha
    assertTrue(UriEscapeType.PATH.isAllowed('A'));
    assertTrue(UriEscapeType.PATH.isAllowed('z'));

    // Tests isDigit
    assertTrue(UriEscapeType.PATH.isAllowed('0'));
    assertTrue(UriEscapeType.PATH.isAllowed('9'));

    // Tests specific unreserved characters: '-', '.', '_', '~'
    assertTrue(UriEscapeType.PATH.isAllowed('-'));
    assertTrue(UriEscapeType.PATH.isAllowed('.'));
    assertTrue(UriEscapeType.PATH.isAllowed('_'));
    assertTrue(UriEscapeType.PATH.isAllowed('~'));
  }

  @Test
  @DisplayName("Verify all sub-delim branches and specific pchar branches")
  void testSubDelimAndPcharBranches() {
    // Tests all sub-delims: '!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '='
    char[] subDelims = {'!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '='};
    for (char c : subDelims) {
      assertTrue(UriEscapeType.PATH.isAllowed(c));
    }

    // Tests specific pchar characters: ':', '@'
    assertTrue(UriEscapeType.PATH.isAllowed(':'));
    assertTrue(UriEscapeType.PATH.isAllowed('@'));

    // Test fallthrough to false
    assertFalse(UriEscapeType.PATH.isAllowed('{'));
  }

  @Test
  @DisplayName("Verify dead code RFC reference methods via Reflection (isReserved, isGenDelim)")
  void testDeadCodeReferenceMethods() throws Exception {
    // The methods isReserved and isGenDelim are provided for RFC completeness
    // but are mathematically bypassed. Reflection is required to hit these branches.
    Method mIsReserved = UriEscapeType.class.getDeclaredMethod("isReserved", int.class);
    mIsReserved.setAccessible(true);

    // Cover isGenDelim branches
    char[] genDelims = {':', '/', '?', '#', '[', ']', '@'};
    for (char c : genDelims) {
      assertTrue((boolean) mIsReserved.invoke(null, c));
    }

    // Cover subDelim fallback branch
    assertTrue((boolean) mIsReserved.invoke(null, '!'));

    // Cover false branch
    assertFalse((boolean) mIsReserved.invoke(null, '{'));
  }

  @Test
  @DisplayName("Verify printHexa byte to hex char array conversion")
  void testPrintHexa() {
    assertArrayEquals(new char[] {'0', '0'}, UriEscapeUtil.printHexa((byte) 0));
    assertArrayEquals(new char[] {'0', 'A'}, UriEscapeUtil.printHexa((byte) 10));
    assertArrayEquals(new char[] {'F', 'F'}, UriEscapeUtil.printHexa((byte) 255));
    assertArrayEquals(new char[] {'8', '0'}, UriEscapeUtil.printHexa((byte) -128));
  }

  @Test
  @DisplayName("Verify fast exit when no escape is required")
  void testEscapeNoOp() {
    String input = "abcABC123-._~";
    String result = UriEscapeUtil.escape(input, UriEscapeType.PATH, "UTF-8");
    assertSame(input, result); // Ensures the exact same object reference is returned
  }

  @Test
  @DisplayName("Verify string slicing and encoding works for start, middle, and end escapes")
  void testEscapeBasic() {
    // Space is %20 in standard URI escaping
    assertEquals("%20abc", UriEscapeUtil.escape(" abc", UriEscapeType.PATH, "UTF-8"));
    assertEquals("a%20b", UriEscapeUtil.escape("a b", UriEscapeType.PATH, "UTF-8"));
    assertEquals("abc%20", UriEscapeUtil.escape("abc ", UriEscapeType.PATH, "UTF-8"));
    assertEquals("a%20b%20c", UriEscapeUtil.escape("a b c", UriEscapeType.PATH, "UTF-8"));
  }

  @Test
  @DisplayName("Verify surrogate pair tracking properly escapes multi-char codepoints")
  void testEscapeSurrogatePair() {
    // 😀 is a high/low surrogate pair (\uD83D\uDE00) -> UTF-8 bytes: F0 9F 98 80
    String input = "a😀b";
    String result = UriEscapeUtil.escape(input, UriEscapeType.PATH, "UTF-8");

    assertEquals("a%F0%9F%98%80b", result);
  }

  @Test
  @DisplayName("Verify IllegalArgumentException thrown on invalid encoding")
  void testEscapeBadEncoding() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> UriEscapeUtil.escape(" ", UriEscapeType.PATH, "INVALID-ENCODING"));
    assertTrue(exception.getMessage().contains("Bad encoding 'INVALID-ENCODING'"));
  }

  @Test
  @DisplayName("Verify private constructor instantiation")
  void testPrivateConstructor() throws Exception {
    Constructor<UriEscapeUtil> constructor = UriEscapeUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    UriEscapeUtil instance = constructor.newInstance();
    assertNotNull(instance);
  }
}
