/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HtmlEscapeLevelTest {

  @Test
  @DisplayName("Verify getEscapeLevel returns correct integer for each constant")
  void testGetEscapeLevel() {
    assertEquals(0, HtmlEscapeLevel.LEVEL_0_ONLY_MARKUP_SIGNIFICANT_EXCEPT_APOS.getEscapeLevel());
    assertEquals(1, HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT.getEscapeLevel());
    assertEquals(2, HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT.getEscapeLevel());
    assertEquals(3, HtmlEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC.getEscapeLevel());
    assertEquals(4, HtmlEscapeLevel.LEVEL_4_ALL_CHARACTERS.getEscapeLevel());
  }

  @Test
  @DisplayName("Verify forLevel returns the correct enum constant for valid levels")
  void testForLevelValid() {
    assertEquals(
        HtmlEscapeLevel.LEVEL_0_ONLY_MARKUP_SIGNIFICANT_EXCEPT_APOS, HtmlEscapeLevel.forLevel(0));
    assertEquals(HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT, HtmlEscapeLevel.forLevel(1));
    assertEquals(
        HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT, HtmlEscapeLevel.forLevel(2));
    assertEquals(HtmlEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC, HtmlEscapeLevel.forLevel(3));
    assertEquals(HtmlEscapeLevel.LEVEL_4_ALL_CHARACTERS, HtmlEscapeLevel.forLevel(4));
  }

  @Test
  @DisplayName("Verify forLevel throws IllegalArgumentException for invalid levels")
  void testForLevelInvalid() {
    IllegalArgumentException ex1 =
        assertThrows(IllegalArgumentException.class, () -> HtmlEscapeLevel.forLevel(-1));
    assertEquals("No escape level enum constant defined for level: -1", ex1.getMessage());

    IllegalArgumentException ex2 =
        assertThrows(IllegalArgumentException.class, () -> HtmlEscapeLevel.forLevel(5));
    assertEquals("No escape level enum constant defined for level: 5", ex2.getMessage());

    IllegalArgumentException ex3 =
        assertThrows(IllegalArgumentException.class, () -> HtmlEscapeLevel.forLevel(99));
    assertEquals("No escape level enum constant defined for level: 99", ex3.getMessage());
  }

  @Test
  @DisplayName("Verify standard enum values() and valueOf() to ensure synthetic method coverage")
  void testEnumSyntheticMethods() {
    // Some coverage tools require hitting the compiler-generated enum methods
    HtmlEscapeLevel[] values = HtmlEscapeLevel.values();
    assertEquals(5, values.length);

    assertEquals(
        HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT,
        HtmlEscapeLevel.valueOf("LEVEL_1_ONLY_MARKUP_SIGNIFICANT"));
  }
}
