/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JsonPointerTest {

  @Test
  @DisplayName(
      "Verify JSON pointer translation including nulls, empty strings, arrays, and standard paths")
  void testJsonPointer() {
    // Branch: path is null
    assertEquals("", JsonPointer.of(null));

    // Branch: path is empty
    assertEquals("", JsonPointer.of(""));

    // Branch: standard property (no array)
    assertEquals("/person/firstName", JsonPointer.of("person.firstName"));

    // Branch: array index matched successfully
    assertEquals("/persons/0/firstName", JsonPointer.of("persons[0].firstName"));

    // Complex nested path
    assertEquals("/users/123/addresses/1/zip", JsonPointer.of("users[123].addresses[1].zip"));

    // Edge case branch: matches array syntax but has non-digit index (fails regex)
    assertEquals("/invalid[abc]/name", JsonPointer.of("invalid[abc].name"));
  }
}
