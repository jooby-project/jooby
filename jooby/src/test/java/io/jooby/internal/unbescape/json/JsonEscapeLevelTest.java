/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JsonEscapeLevelTest {

  @Test
  @DisplayName("Verify getEscapeLevel returns correct integer for each constant")
  void testGetEscapeLevel() {
    assertEquals(1, JsonEscapeLevel.LEVEL_1_BASIC_ESCAPE_SET.getEscapeLevel());
    assertEquals(2, JsonEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_BASIC_ESCAPE_SET.getEscapeLevel());
    assertEquals(3, JsonEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC.getEscapeLevel());
    assertEquals(4, JsonEscapeLevel.LEVEL_4_ALL_CHARACTERS.getEscapeLevel());
  }

  @Test
  @DisplayName("Verify forLevel returns the correct enum constant for valid levels")
  void testForLevelValid() {
    assertEquals(JsonEscapeLevel.LEVEL_1_BASIC_ESCAPE_SET, JsonEscapeLevel.forLevel(1));
    assertEquals(
        JsonEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_BASIC_ESCAPE_SET, JsonEscapeLevel.forLevel(2));
    assertEquals(JsonEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC, JsonEscapeLevel.forLevel(3));
    assertEquals(JsonEscapeLevel.LEVEL_4_ALL_CHARACTERS, JsonEscapeLevel.forLevel(4));
  }

  @Test
  @DisplayName("Verify forLevel throws IllegalArgumentException for invalid levels")
  void testForLevelInvalid() {
    IllegalArgumentException ex1 =
        assertThrows(IllegalArgumentException.class, () -> JsonEscapeLevel.forLevel(0));
    assertEquals("No escape level enum constant defined for level: 0", ex1.getMessage());

    IllegalArgumentException ex2 =
        assertThrows(IllegalArgumentException.class, () -> JsonEscapeLevel.forLevel(5));
    assertEquals("No escape level enum constant defined for level: 5", ex2.getMessage());

    IllegalArgumentException ex3 =
        assertThrows(IllegalArgumentException.class, () -> JsonEscapeLevel.forLevel(-99));
    assertEquals("No escape level enum constant defined for level: -99", ex3.getMessage());
  }

  @Test
  @DisplayName("Verify standard enum values() and valueOf() to ensure synthetic method coverage")
  void testEnumSyntheticMethods() {
    // Some strict coverage tools (like JaCoCo) require hitting the compiler-generated enum methods
    JsonEscapeLevel[] values = JsonEscapeLevel.values();
    assertEquals(4, values.length);

    assertEquals(
        JsonEscapeLevel.LEVEL_1_BASIC_ESCAPE_SET,
        JsonEscapeLevel.valueOf("LEVEL_1_BASIC_ESCAPE_SET"));
  }
}
