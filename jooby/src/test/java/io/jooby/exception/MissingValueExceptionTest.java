/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MissingValueExceptionTest {

  @Test
  @DisplayName("Verify constructor and getName accurately store and return the parameter name")
  void testConstructorAndGetter() {
    String paramName = "userId";
    MissingValueException exception = new MissingValueException(paramName);

    assertEquals(paramName, exception.getName());
    assertEquals("Missing value: 'userId'", exception.getMessage());
  }

  @Test
  @DisplayName("requireNonNull should return the value if it is not null")
  void testRequireNonNullSuccess() {
    String name = "username";
    String value = "edgar";

    String result = MissingValueException.requireNonNull(name, value);

    assertEquals(value, result);
  }

  @Test
  @DisplayName("requireNonNull should throw MissingValueException if the value is null")
  void testRequireNonNullFailure() {
    String name = "apiKey";

    MissingValueException ex =
        assertThrows(
            MissingValueException.class, () -> MissingValueException.requireNonNull(name, null));

    assertEquals(name, ex.getName());
    assertEquals("Missing value: 'apiKey'", ex.getMessage());
  }
}
