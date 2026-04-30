/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class JsonRpcExceptionTest {

  @Test
  void shouldConstructWithCodeAndMessage() {
    JsonRpcErrorCode mockCode = mock(JsonRpcErrorCode.class);
    when(mockCode.getCode()).thenReturn(-32600);
    when(mockCode.getMessage()).thenReturn("Default Error"); // Used as fallback in ErrorDetail

    JsonRpcException ex = new JsonRpcException(mockCode, "Custom message");

    assertEquals(mockCode, ex.getCode());
    assertEquals("Custom message", ex.getMessage());
    assertNull(ex.getData());
    assertNull(ex.getCause());

    JsonRpcResponse.ErrorDetail detail = ex.toErrorDetail();
    assertEquals(-32600, detail.getCode());
    assertEquals("Custom message", detail.getMessage());
    assertNull(detail.getData());
    assertNull(detail.exception());
  }

  @Test
  void shouldConstructWithCodeAndCause() {
    JsonRpcErrorCode mockCode = mock(JsonRpcErrorCode.class);
    when(mockCode.getCode()).thenReturn(-32603);
    when(mockCode.getMessage()).thenReturn("Internal error");

    Throwable cause = new RuntimeException("Database down");
    JsonRpcException ex = new JsonRpcException(mockCode, cause);

    assertEquals(mockCode, ex.getCode());
    // The message is inherited from the code's default message
    assertEquals("Internal error", ex.getMessage());
    assertNull(ex.getData());
    assertEquals(cause, ex.getCause());

    JsonRpcResponse.ErrorDetail detail = ex.toErrorDetail();
    assertEquals(-32603, detail.getCode());
    assertEquals("Internal error", detail.getMessage());
    // The cause correctly populates the data field in ErrorDetail
    assertEquals("Database down", detail.getData());
    assertEquals(cause, detail.exception());
  }

  @Test
  void shouldConstructWithCodeMessageAndCause() {
    JsonRpcErrorCode mockCode = mock(JsonRpcErrorCode.class);
    when(mockCode.getCode()).thenReturn(-32000);
    when(mockCode.getMessage()).thenReturn("Server Error Fallback");

    Throwable cause = new IllegalArgumentException("Bad Argument");
    JsonRpcException ex = new JsonRpcException(mockCode, "Specific message", cause);

    assertEquals(mockCode, ex.getCode());
    assertEquals("Specific message", ex.getMessage());
    assertNull(ex.getData());
    assertEquals(cause, ex.getCause());

    JsonRpcResponse.ErrorDetail detail = ex.toErrorDetail();
    assertEquals(-32000, detail.getCode());
    assertEquals("Specific message", detail.getMessage());

    // The cause becomes the data since explicit data is null
    assertEquals("Bad Argument", detail.getData());
    assertEquals(cause, detail.exception());
  }

  @Test
  void shouldConstructWithCodeMessageAndData() {
    JsonRpcErrorCode mockCode = mock(JsonRpcErrorCode.class);
    when(mockCode.getCode()).thenReturn(-32602);
    when(mockCode.getMessage()).thenReturn("Invalid params fallback");

    Object customData = "Validation Failed for Field X";
    JsonRpcException ex = new JsonRpcException(mockCode, "Invalid params", customData);

    assertEquals(mockCode, ex.getCode());
    assertEquals("Invalid params", ex.getMessage());
    assertEquals(customData, ex.getData());
    assertNull(ex.getCause());

    JsonRpcResponse.ErrorDetail detail = ex.toErrorDetail();
    assertEquals(-32602, detail.getCode());
    assertEquals("Invalid params", detail.getMessage());

    // Explicit data takes precedence over cause
    assertEquals("Validation Failed for Field X", detail.getData());
    assertNull(detail.exception());
  }
}
