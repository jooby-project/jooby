/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.jooby.StatusCode;

@SuppressWarnings("unchecked")
class TrpcExceptionTest {

  // --- CONSTRUCTOR TESTS ---

  @Test
  void testConstructor_ProcedureAndStatusCodeAndCause() {
    Throwable cause = new RuntimeException("Underlying database failure");
    TrpcErrorCode mockErrorCode = mock(TrpcErrorCode.class);
    when(mockErrorCode.name()).thenReturn("INTERNAL_SERVER_ERROR");

    // Intercept the static TrpcErrorCode.of() resolution
    try (MockedStatic<TrpcErrorCode> trpcErrorCodeMock = mockStatic(TrpcErrorCode.class)) {
      trpcErrorCodeMock
          .when(() -> TrpcErrorCode.of(StatusCode.SERVER_ERROR))
          .thenReturn(mockErrorCode);

      TrpcException ex = new TrpcException("movies.list", StatusCode.SERVER_ERROR, cause);

      assertEquals("movies.list: INTERNAL_SERVER_ERROR", ex.getMessage());
      assertEquals(cause, ex.getCause());
      assertEquals("movies.list", ex.getProcedure());
    }
  }

  @Test
  void testConstructor_ProcedureAndTrpcErrorCodeAndCause() {
    Throwable cause = new IllegalArgumentException("Invalid ID format");
    TrpcErrorCode mockErrorCode = mock(TrpcErrorCode.class);
    when(mockErrorCode.name()).thenReturn("BAD_REQUEST");
    when(mockErrorCode.getStatusCode()).thenReturn(StatusCode.BAD_REQUEST);

    TrpcException ex = new TrpcException("users.getById", mockErrorCode, cause);

    assertEquals("users.getById: BAD_REQUEST", ex.getMessage());
    assertEquals(cause, ex.getCause());
    assertEquals("users.getById", ex.getProcedure());
    assertEquals(StatusCode.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void testConstructor_ProcedureAndStatusCode() {
    TrpcErrorCode mockErrorCode = mock(TrpcErrorCode.class);
    when(mockErrorCode.name()).thenReturn("NOT_FOUND");

    try (MockedStatic<TrpcErrorCode> trpcErrorCodeMock = mockStatic(TrpcErrorCode.class)) {
      trpcErrorCodeMock
          .when(() -> TrpcErrorCode.of(StatusCode.NOT_FOUND))
          .thenReturn(mockErrorCode);

      TrpcException ex = new TrpcException("posts.delete", StatusCode.NOT_FOUND);

      assertEquals("posts.delete: NOT_FOUND", ex.getMessage());
      assertNull(ex.getCause());
      assertEquals("posts.delete", ex.getProcedure());
    }
  }

  @Test
  void testConstructor_ProcedureAndTrpcErrorCode() {
    TrpcErrorCode mockErrorCode = mock(TrpcErrorCode.class);
    when(mockErrorCode.name()).thenReturn("UNAUTHORIZED");
    when(mockErrorCode.getStatusCode()).thenReturn(StatusCode.UNAUTHORIZED);

    TrpcException ex = new TrpcException("admin.dashboard", mockErrorCode);

    assertEquals("admin.dashboard: UNAUTHORIZED", ex.getMessage());
    assertNull(ex.getCause());
    assertEquals("admin.dashboard", ex.getProcedure());
    assertEquals(StatusCode.UNAUTHORIZED, ex.getStatusCode());
  }

  // --- TO MAP (JSON ENVELOPE) TESTS ---

  @Test
  void testToMap_WithCauseMessage_UsesCauseMessage() {
    Throwable cause = new RuntimeException("Specific validation failed on field 'email'");
    TrpcErrorCode mockErrorCode = mock(TrpcErrorCode.class);
    when(mockErrorCode.name()).thenReturn("BAD_REQUEST");
    when(mockErrorCode.getStatusCode()).thenReturn(StatusCode.BAD_REQUEST);
    when(mockErrorCode.getRpcCode()).thenReturn(-32600);

    TrpcException ex = new TrpcException("users.create", mockErrorCode, cause);

    Map<String, Object> map = ex.toMap();

    assertNotNull(map);
    assertTrue(map.containsKey("error"));

    Map<String, Object> error = (Map<String, Object>) map.get("error");

    // Verifies the message was extracted from the cause
    assertEquals("Specific validation failed on field 'email'", error.get("message"));
    assertEquals(-32600, error.get("code"));

    Map<String, Object> data = (Map<String, Object>) error.get("data");
    assertEquals("BAD_REQUEST", data.get("code"));
    assertEquals(400, data.get("httpStatus"));
    assertEquals("users.create", data.get("path"));
  }

  @Test
  void testToMap_WithCauseButNullMessage_FallsBackToErrorCodeName() {
    // Cause is provided, but getMessage() will return null
    Throwable cause = new NullPointerException();
    TrpcErrorCode mockErrorCode = mock(TrpcErrorCode.class);
    when(mockErrorCode.name()).thenReturn("INTERNAL_SERVER_ERROR");
    when(mockErrorCode.getStatusCode()).thenReturn(StatusCode.SERVER_ERROR);
    when(mockErrorCode.getRpcCode()).thenReturn(-32603);

    TrpcException ex = new TrpcException("system.ping", mockErrorCode, cause);

    Map<String, Object> map = ex.toMap();
    Map<String, Object> error = (Map<String, Object>) map.get("error");

    // Verifies it correctly fell back to the error code name
    assertEquals("INTERNAL_SERVER_ERROR", error.get("message"));
  }

  @Test
  void testToMap_WithoutCause_FallsBackToErrorCodeName() {
    TrpcErrorCode mockErrorCode = mock(TrpcErrorCode.class);
    when(mockErrorCode.name()).thenReturn("FORBIDDEN");
    when(mockErrorCode.getStatusCode()).thenReturn(StatusCode.FORBIDDEN);
    when(mockErrorCode.getRpcCode()).thenReturn(-32003);

    TrpcException ex = new TrpcException("billing.charge", mockErrorCode);

    Map<String, Object> map = ex.toMap();
    Map<String, Object> error = (Map<String, Object>) map.get("error");

    // Verifies it correctly fell back to the error code name when cause is completely null
    assertEquals("FORBIDDEN", error.get("message"));

    Map<String, Object> data = (Map<String, Object>) error.get("data");
    assertEquals("FORBIDDEN", data.get("code"));
    assertEquals(403, data.get("httpStatus"));
    assertEquals("billing.charge", data.get("path"));
  }
}
