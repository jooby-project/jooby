/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonRpcResponseTest {

  @Test
  void shouldCreateSuccessResponse() {
    Object result = "Success Payload";
    JsonRpcResponse response = JsonRpcResponse.success(100, result);

    assertEquals("2.0", response.getJsonrpc());
    assertEquals(100, response.getId());
    assertEquals(result, response.getResult());
    assertNull(response.getError());
  }

  @Test
  void shouldCreateErrorResponseWithStandardObjectData() {
    JsonRpcErrorCode mockCode = mock(JsonRpcErrorCode.class);
    when(mockCode.getCode()).thenReturn(-32600);
    when(mockCode.getMessage()).thenReturn("Invalid Request");

    Object errorData = "Custom Error Detail";
    JsonRpcResponse response = JsonRpcResponse.error(101, mockCode, errorData);

    assertEquals("2.0", response.getJsonrpc());
    assertEquals(101, response.getId());
    assertNull(response.getResult());

    JsonRpcResponse.ErrorDetail error = response.getError();
    assertNotNull(error);
    assertEquals(-32600, error.getCode());
    assertEquals("Invalid Request", error.getMessage());
    assertEquals("Custom Error Detail", error.getData());
    assertNull(error.exception());
  }

  @Test
  void shouldDelegateToThrowableMethodWhenDataIsThrowable() {
    JsonRpcErrorCode mockCode = mock(JsonRpcErrorCode.class);
    when(mockCode.getCode()).thenReturn(-32603);
    when(mockCode.getMessage()).thenReturn("Internal error");

    Throwable cause = new RuntimeException("Database Timeout");
    // Pass throwable as a generic Object to hit the "instanceof Throwable" true branch
    Object dataAsObject = cause;

    JsonRpcResponse response = JsonRpcResponse.error(102, mockCode, dataAsObject);

    JsonRpcResponse.ErrorDetail error = response.getError();
    assertNotNull(error);
    assertEquals(-32603, error.getCode());
    assertEquals("Internal error", error.getMessage());

    // Validates that it prevents stack trace leakage by only exposing the message
    assertEquals("Database Timeout", error.getData());
    assertEquals(cause, error.exception());
  }

  @Test
  void shouldCreateErrorResponseWithDirectThrowable() {
    JsonRpcErrorCode mockCode = mock(JsonRpcErrorCode.class);
    when(mockCode.getCode()).thenReturn(-32000);
    when(mockCode.getMessage()).thenReturn("Server error");

    Throwable cause = new IllegalArgumentException("Bad Argument");

    // Null ID implies a parsing error or invalid request before ID extraction
    JsonRpcResponse response = JsonRpcResponse.error(null, mockCode, cause);

    assertNull(response.getId());
    JsonRpcResponse.ErrorDetail error = response.getError();
    assertNotNull(error);
    assertEquals("Bad Argument", error.getData());
    assertEquals(cause, error.exception());
  }

  @Test
  void errorDetailConstructorsAndMessageFallback() {
    JsonRpcErrorCode mockCode = mock(JsonRpcErrorCode.class);
    when(mockCode.getCode()).thenReturn(-32700);
    when(mockCode.getMessage()).thenReturn("Parse error");

    // 1. One-arg constructor
    JsonRpcResponse.ErrorDetail detail1 = new JsonRpcResponse.ErrorDetail(mockCode);
    assertEquals(-32700, detail1.getCode());
    assertEquals("Parse error", detail1.getMessage());
    assertNull(detail1.getData());
    assertNull(detail1.exception());

    // 2. Three-arg constructor with explicitly overridden message
    JsonRpcResponse.ErrorDetail detail3 =
        new JsonRpcResponse.ErrorDetail(mockCode, "Custom Parse Failure", null);
    assertEquals(-32700, detail3.getCode());
    assertEquals("Custom Parse Failure", detail3.getMessage());
  }

  @Test
  void shouldApplyGettersAndSettersProperly() {
    JsonRpcResponse response = JsonRpcResponse.success(1, "res");

    // Test Setters
    response.setJsonrpc("1.0");
    response.setId("req-xyz");
    response.setResult(999);

    JsonRpcErrorCode mockCode = mock(JsonRpcErrorCode.class);
    when(mockCode.getCode()).thenReturn(-32099);
    JsonRpcResponse.ErrorDetail error = new JsonRpcResponse.ErrorDetail(mockCode);
    response.setError(error);

    // Test Getters
    assertEquals("1.0", response.getJsonrpc());
    assertEquals("req-xyz", response.getId());
    assertEquals(999, response.getResult());
    assertEquals(error, response.getError());
  }
}
