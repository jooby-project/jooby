/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.jooby.exception.MissingValueException;

class JsonRpcReaderTest {

  @Test
  void requireNextShouldThrowMissingValueExceptionWhenNull() {
    JsonRpcReader reader = mock(JsonRpcReader.class);

    // Setup the mock to simulate a missing parameter
    when(reader.nextIsNull("targetParam")).thenReturn(true);

    // Tell Mockito to execute the actual default method logic in the interface
    doCallRealMethod().when(reader).requireNext(anyString());

    // Assert the fast-fail exception is thrown
    assertThrows(MissingValueException.class, () -> reader.requireNext("targetParam"));

    // Verify the internal check was actually made
    verify(reader).nextIsNull("targetParam");
  }

  @Test
  void requireNextShouldDoNothingWhenNotNull() {
    JsonRpcReader reader = mock(JsonRpcReader.class);

    // Setup the mock to simulate a present parameter
    when(reader.nextIsNull("targetParam")).thenReturn(false);

    // Tell Mockito to execute the actual default method logic
    doCallRealMethod().when(reader).requireNext(anyString());

    // Assert that execution proceeds normally without throwing
    assertDoesNotThrow(() -> reader.requireNext("targetParam"));

    // Verify the internal check was actually made
    verify(reader).nextIsNull("targetParam");
  }
}
