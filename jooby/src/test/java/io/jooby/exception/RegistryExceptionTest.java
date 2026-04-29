/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.StatusCode;

class RegistryExceptionTest {

  @Test
  @DisplayName("Verify constructor with message sets status to 500 and stores message")
  void testConstructorWithMessage() {
    String message = "Service not found: MyService";
    RegistryException exception = new RegistryException(message);

    assertEquals(StatusCode.SERVER_ERROR, exception.getStatusCode());
    assertEquals(message, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("Verify constructor with message and cause sets status to 500 and stores both")
  void testConstructorWithMessageAndCause() {
    String message = "Dependency injection failed";
    Throwable cause = new RuntimeException("Root cause error");
    RegistryException exception = new RegistryException(message, cause);

    assertEquals(StatusCode.SERVER_ERROR, exception.getStatusCode());
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }
}
