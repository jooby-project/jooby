/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.StatusCode;

class TrpcErrorCodeTest {

  @Test
  void testEnumProperties() {
    // Test standard properties
    assertEquals(-32600, TrpcErrorCode.BAD_REQUEST.getRpcCode());
    assertEquals(StatusCode.BAD_REQUEST, TrpcErrorCode.BAD_REQUEST.getStatusCode());

    assertEquals(-32700, TrpcErrorCode.PARSE_ERROR.getRpcCode());
    assertEquals(StatusCode.BAD_REQUEST, TrpcErrorCode.PARSE_ERROR.getStatusCode());
  }

  @Test
  void testOf_ExactMatch() {
    // Test standard 1:1 mappings
    assertEquals(TrpcErrorCode.UNAUTHORIZED, TrpcErrorCode.of(StatusCode.UNAUTHORIZED));
    assertEquals(TrpcErrorCode.FORBIDDEN, TrpcErrorCode.of(StatusCode.FORBIDDEN));
    assertEquals(TrpcErrorCode.NOT_FOUND, TrpcErrorCode.of(StatusCode.NOT_FOUND));
    assertEquals(
        TrpcErrorCode.METHOD_NOT_SUPPORTED, TrpcErrorCode.of(StatusCode.METHOD_NOT_ALLOWED));
    assertEquals(TrpcErrorCode.TIMEOUT, TrpcErrorCode.of(StatusCode.REQUEST_TIMEOUT));
    assertEquals(TrpcErrorCode.CONFLICT, TrpcErrorCode.of(StatusCode.CONFLICT));
    assertEquals(
        TrpcErrorCode.PRECONDITION_FAILED, TrpcErrorCode.of(StatusCode.PRECONDITION_FAILED));
    assertEquals(
        TrpcErrorCode.PAYLOAD_TOO_LARGE, TrpcErrorCode.of(StatusCode.REQUEST_ENTITY_TOO_LARGE));
    assertEquals(
        TrpcErrorCode.UNPROCESSABLE_CONTENT, TrpcErrorCode.of(StatusCode.UNPROCESSABLE_ENTITY));
    assertEquals(TrpcErrorCode.TOO_MANY_REQUESTS, TrpcErrorCode.of(StatusCode.TOO_MANY_REQUESTS));
    assertEquals(
        TrpcErrorCode.CLIENT_CLOSED_REQUEST, TrpcErrorCode.of(StatusCode.CLIENT_CLOSED_REQUEST));
    assertEquals(TrpcErrorCode.INTERNAL_SERVER_ERROR, TrpcErrorCode.of(StatusCode.SERVER_ERROR));
  }

  @Test
  void testOf_DuplicateHttpStatusCodeResolution() {
    // Both BAD_REQUEST and PARSE_ERROR map to HTTP 400.
    // The `of()` method should return the first one declared in the enum (BAD_REQUEST).
    assertEquals(TrpcErrorCode.BAD_REQUEST, TrpcErrorCode.of(StatusCode.BAD_REQUEST));
  }

  @Test
  void testOf_FallbackToInternalServerError() {
    // Status codes that are not explicitly mapped should fallback to INTERNAL_SERVER_ERROR
    assertEquals(TrpcErrorCode.INTERNAL_SERVER_ERROR, TrpcErrorCode.of(StatusCode.OK));
    assertEquals(TrpcErrorCode.INTERNAL_SERVER_ERROR, TrpcErrorCode.of(StatusCode.CREATED));
    assertEquals(TrpcErrorCode.INTERNAL_SERVER_ERROR, TrpcErrorCode.of(StatusCode.BAD_GATEWAY));
  }

  @Test
  void testEnumValues() {
    // Guarantees coverage for implicitly generated enum methods
    TrpcErrorCode[] values = TrpcErrorCode.values();
    assertEquals(14, values.length);

    assertEquals(TrpcErrorCode.FORBIDDEN, TrpcErrorCode.valueOf("FORBIDDEN"));
  }
}
