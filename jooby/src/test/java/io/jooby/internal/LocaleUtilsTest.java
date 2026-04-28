/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LocaleUtilsTest {

  @Test
  @DisplayName("Test parseRanges branches: null, valid sorting, trailing semicolon, and exceptions")
  void testParseRanges() {
    // 1. Null branch
    assertEquals(Optional.empty(), LocaleUtils.parseRanges(null));

    // 2. Valid string with mixed weights (Verifies descending sort order branch)
    // "en-US" gets default weight 1.0. "en" is 0.9, "es" is 0.8
    Optional<List<Locale.LanguageRange>> ranges =
        LocaleUtils.parseRanges("es;q=0.8,en-US,en;q=0.9");
    assertTrue(ranges.isPresent());
    List<Locale.LanguageRange> list = ranges.get();
    assertEquals(3, list.size());
    assertEquals("en-us", list.get(0).getRange()); // 1.0
    assertEquals("en", list.get(1).getRange()); // 0.9
    assertEquals("es", list.get(2).getRange()); // 0.8

    // 3. Trailing semicolon branch (ends with ';')
    Optional<List<Locale.LanguageRange>> trailing = LocaleUtils.parseRanges("en-US;q=0.8;");
    assertTrue(trailing.isPresent());
    assertEquals(1, trailing.get().size());
    assertEquals("en-us", trailing.get().get(0).getRange());
    assertEquals(0.8, trailing.get().get(0).getWeight());

    // 4. Exception catch branch (IllegalArgumentException -> Optional.empty)
    // Triggers exception natively via weight > 1.0 (Valid weights are 0.0 to 1.0)
    assertEquals(Optional.empty(), LocaleUtils.parseRanges("en;q=2.0"));

    // (Note: Removed the "a b c" assertion as Java's parser unexpectedly strips spaces and accepts
    // it)
  }

  @Test
  @DisplayName("Test parseLocales mappings")
  void testParseLocales() {
    // Valid branch
    Optional<List<Locale>> locales = LocaleUtils.parseLocales("es-AR,en-US;q=0.8");
    assertTrue(locales.isPresent());
    assertEquals(2, locales.get().size());

    // Verifies the map sequence translates LanguageRanges into Locale objects
    assertEquals(Locale.forLanguageTag("es-AR"), locales.get().get(0));
    assertEquals(Locale.forLanguageTag("en-US"), locales.get().get(1));

    // Invalid branch (propagates Optional.empty correctly using an invalid weight)
    assertFalse(LocaleUtils.parseLocales("en;q=invalid").isPresent());
  }

  @Test
  @DisplayName("Test parseLocalesOrFail success and custom exception branches")
  void testParseLocalesOrFail() {
    // Valid branch
    List<Locale> locales = LocaleUtils.parseLocalesOrFail("es-AR");
    assertEquals(1, locales.size());
    assertEquals(Locale.forLanguageTag("es-AR"), locales.get(0));

    // Exception branch (.orElseThrow)
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              LocaleUtils.parseLocalesOrFail("en;q=invalid");
            });

    // Verifies the custom formatted message was constructed correctly
    assertTrue(ex.getMessage().contains("Invalid value 'en;q=invalid'"));
    assertTrue(ex.getMessage().contains("java.util.Locale$LanguageRange"));
  }
}
