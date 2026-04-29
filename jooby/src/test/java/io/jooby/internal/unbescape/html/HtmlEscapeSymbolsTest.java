/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.html;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HtmlEscapeSymbolsTest {

  @Test
  @DisplayName(
      "Verify HtmlEscapeSymbols constructor and inner structures (Overflow, Double Codepoints,"
          + " Collisions)")
  void testConstructorAndInitialization() throws Exception {
    HtmlEscapeSymbols.References refs = new HtmlEscapeSymbols.References();

    // 1. Standard single codepoint
    refs.addReference(100, "&test;");

    // 2. Collision testing: Same codepoint, different NCR.
    // The class uses the order of insertion to prioritize the primary NCR.
    refs.addReference(100, "&test2;");

    // Reverse collision logic (earlier alphabetical but inserted later)
    refs.addReference(101, "&testB;");
    refs.addReference(101, "&testA;");

    // 3. Double codepoint
    refs.addReference(200, 201, "&double;");

    // 4. Overflow codepoint (>= 0x2FFF / 12287)
    refs.addReference(15000, "&overflow;");

    byte[] escapeLevels = new byte[HtmlEscapeSymbols.MAX_ASCII_CHAR + 2];
    escapeLevels[50] = 1; // Arbitrary marker

    HtmlEscapeSymbols symbols = new HtmlEscapeSymbols(refs, escapeLevels);

    // Verify ESCAPE_LEVELS array copy
    assertEquals(1, symbols.ESCAPE_LEVELS[50]);

    // Verify NCRS_BY_CODEPOINT populated
    assertTrue(symbols.NCRS_BY_CODEPOINT[100] != HtmlEscapeSymbols.NO_NCR);
    assertTrue(symbols.NCRS_BY_CODEPOINT[101] != HtmlEscapeSymbols.NO_NCR);

    // Verify OVERFLOW populated
    assertNotNull(symbols.NCRS_BY_CODEPOINT_OVERFLOW);
    assertEquals(1, symbols.NCRS_BY_CODEPOINT_OVERFLOW.size());
    assertTrue(symbols.NCRS_BY_CODEPOINT_OVERFLOW.containsKey(15000));

    // Verify DOUBLE_CODEPOINTS populated
    assertNotNull(symbols.DOUBLE_CODEPOINTS);
    assertEquals(1, symbols.DOUBLE_CODEPOINTS.length);
    assertArrayEquals(new int[] {200, 201}, symbols.DOUBLE_CODEPOINTS[0]);
  }

  @Test
  @DisplayName("Verify instantiation branches where overflow and double codepoints are NOT needed")
  void testConstructorWithoutOverflowOrDoubles() {
    HtmlEscapeSymbols.References emptyRefs = new HtmlEscapeSymbols.References();
    emptyRefs.addReference(50, "&simple;");

    HtmlEscapeSymbols symbols = new HtmlEscapeSymbols(emptyRefs, new byte[130]);

    assertNull(symbols.NCRS_BY_CODEPOINT_OVERFLOW);
    assertNull(symbols.DOUBLE_CODEPOINTS);
  }

  @Test
  @DisplayName("Verify RuntimeException on unsupported reference lengths")
  void testUnsupportedReferenceLength() throws Exception {
    HtmlEscapeSymbols.References badRefs = new HtmlEscapeSymbols.References();

    // Use reflection to bypass the References API and force an invalid 3-codepoint sequence
    Class<?> refClass =
        Class.forName("io.jooby.internal.unbescape.html.HtmlEscapeSymbols$Reference");
    Constructor<?> refConstructor = refClass.getDeclaredConstructor(String.class, int[].class);
    refConstructor.setAccessible(true);
    Object badRef = refConstructor.newInstance("&bad;", new int[] {1, 2, 3});

    Field listField = HtmlEscapeSymbols.References.class.getDeclaredField("references");
    listField.setAccessible(true);
    @SuppressWarnings("unchecked")
    List<Object> list = (List<Object>) listField.get(badRefs);
    list.add(badRef);

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> new HtmlEscapeSymbols(badRefs, new byte[130]));
    assertTrue(ex.getMessage().contains("Unsupported codepoints #: 3"));
  }

  @Test
  @DisplayName("Verify private positionInList fallback branch")
  void testPositionInListFallback() throws Exception {
    Method method =
        HtmlEscapeSymbols.class.getDeclaredMethod("positionInList", List.class, char[].class);
    method.setAccessible(true);

    List<char[]> list = new ArrayList<>();
    list.add(new char[] {'a'});

    // Not found in list branch
    int result = (int) method.invoke(null, list, new char[] {'b'});
    assertEquals(-1, result);
  }

  @Test
  @DisplayName("Verify all edge cases of the custom compare() logic for Strings")
  void testCompareString() throws Exception {
    Method compare =
        HtmlEscapeSymbols.class.getDeclaredMethod(
            "compare", char[].class, String.class, int.class, int.class);
    compare.setAccessible(true);

    // Exact Match
    assertEquals(0, (int) compare.invoke(null, "&a;".toCharArray(), "&a;", 0, 3));

    // ncr[i] < tc
    // branch: tc == ';'
    assertEquals(1, (int) compare.invoke(null, "&1".toCharArray(), "&;", 0, 2));
    // branch: tc != ';'
    assertEquals(-1, (int) compare.invoke(null, "&a;".toCharArray(), "&b;", 0, 3));

    // ncr[i] > tc
    // branch: ncr[i] == ';'
    assertEquals(-1, (int) compare.invoke(null, "&;".toCharArray(), "&1", 0, 2));
    // branch: ncr[i] != ';'
    assertEquals(1, (int) compare.invoke(null, "&b;".toCharArray(), "&a;", 0, 3));

    // ncr.length > i
    // branch: ncr[i] == ';'
    assertEquals(-1, (int) compare.invoke(null, "&a;".toCharArray(), "&a", 0, 2));
    // branch: ncr[i] != ';'
    assertEquals(1, (int) compare.invoke(null, "&aX".toCharArray(), "&a", 0, 2));

    // textLen > i
    // branch: tc == ';'
    assertEquals(1, (int) compare.invoke(null, "&a".toCharArray(), "&a;", 0, 3));
    // branch: tc != ';' (Triggers partial match formula: -((textLen - i) + 10))
    // len=3, i=2 -> -((3 - 2) + 10) = -11
    assertEquals(-11, (int) compare.invoke(null, "&a".toCharArray(), "&aX", 0, 3));
  }

  @Test
  @DisplayName("Verify all edge cases of the custom compare() logic for char arrays")
  void testCompareCharArray() throws Exception {
    Method compare =
        HtmlEscapeSymbols.class.getDeclaredMethod(
            "compare", char[].class, char[].class, int.class, int.class);
    compare.setAccessible(true);

    assertEquals(0, (int) compare.invoke(null, "&a;".toCharArray(), "&a;".toCharArray(), 0, 3));
    assertEquals(1, (int) compare.invoke(null, "&1".toCharArray(), "&;".toCharArray(), 0, 2));
    assertEquals(-1, (int) compare.invoke(null, "&a;".toCharArray(), "&b;".toCharArray(), 0, 3));
    assertEquals(-1, (int) compare.invoke(null, "&;".toCharArray(), "&1".toCharArray(), 0, 2));
    assertEquals(1, (int) compare.invoke(null, "&b;".toCharArray(), "&a;".toCharArray(), 0, 3));
    assertEquals(-1, (int) compare.invoke(null, "&a;".toCharArray(), "&a".toCharArray(), 0, 2));
    assertEquals(1, (int) compare.invoke(null, "&aX".toCharArray(), "&a".toCharArray(), 0, 2));
    assertEquals(1, (int) compare.invoke(null, "&a".toCharArray(), "&a;".toCharArray(), 0, 3));
    assertEquals(-11, (int) compare.invoke(null, "&a".toCharArray(), "&aX".toCharArray(), 0, 3));
  }

  @Test
  @DisplayName(
      "Verify custom binarySearch logic including exact, miss, and partial match weighting")
  void testBinarySearch() {
    char[][] values = {
      "&a".toCharArray(),
      "&a;".toCharArray(),
      "&aa".toCharArray(), // Added for partial testing collision
      "&b;".toCharArray(),
      "&c;".toCharArray()
    };

    // String variant
    assertEquals(3, HtmlEscapeSymbols.binarySearch(values, "&b;", 0, 3)); // Exact match
    assertEquals(
        Integer.MIN_VALUE, HtmlEscapeSymbols.binarySearch(values, "&0;", 0, 3)); // Miss (Left out)
    assertEquals(
        Integer.MIN_VALUE, HtmlEscapeSymbols.binarySearch(values, "&z;", 0, 3)); // Miss (Right out)

    // Partial match string test. Searching for "&aX" partially matches "&a" at index 0.
    // (-1) * (index + 10) -> (-1) * (0 + 10) = -10
    assertEquals(-10, HtmlEscapeSymbols.binarySearch(values, "&aX", 0, 3));

    // char[] variant
    assertEquals(
        3, HtmlEscapeSymbols.binarySearch(values, "&b;".toCharArray(), 0, 3)); // Exact match
    assertEquals(
        Integer.MIN_VALUE,
        HtmlEscapeSymbols.binarySearch(values, "&0;".toCharArray(), 0, 3)); // Miss

    // Partial match char[] test
    assertEquals(-10, HtmlEscapeSymbols.binarySearch(values, "&aX".toCharArray(), 0, 3));
  }
}
