/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.StatusCode;

class JsonRpcErrorCodeTest {

  @Test
  void testCoreProtocolGetters() {
    JsonRpcErrorCode code = JsonRpcErrorCode.INVALID_REQUEST;

    assertEquals(-32600, code.getCode());
    assertEquals("Invalid Request", code.getMessage());
    assertEquals(StatusCode.BAD_REQUEST, code.getStatusCode());
    assertTrue(code.isProtocol());
  }

  @Test
  void testImplementationDefinedGetters() {
    JsonRpcErrorCode code = JsonRpcErrorCode.NOT_FOUND_ERROR;

    assertEquals(-32004, code.getCode());
    assertEquals("Not found", code.getMessage());
    assertEquals(StatusCode.NOT_FOUND, code.getStatusCode());
    assertFalse(code.isProtocol());
  }

  @Test
  void testOfResolvesImplementationDefinedErrors() {
    // These should successfully match against the !protocol condition in the loop
    assertEquals(JsonRpcErrorCode.UNAUTHORIZED, JsonRpcErrorCode.of(StatusCode.UNAUTHORIZED));
    assertEquals(JsonRpcErrorCode.FORBIDDEN, JsonRpcErrorCode.of(StatusCode.FORBIDDEN));
    assertEquals(JsonRpcErrorCode.NOT_FOUND_ERROR, JsonRpcErrorCode.of(StatusCode.NOT_FOUND));
    assertEquals(JsonRpcErrorCode.CONFLICT, JsonRpcErrorCode.of(StatusCode.CONFLICT));
    assertEquals(
        JsonRpcErrorCode.PRECONDITION_FAILED, JsonRpcErrorCode.of(StatusCode.PRECONDITION_FAILED));
    assertEquals(
        JsonRpcErrorCode.UNPROCESSABLE_CONTENT,
        JsonRpcErrorCode.of(StatusCode.UNPROCESSABLE_ENTITY));
    assertEquals(
        JsonRpcErrorCode.TOO_MANY_REQUESTS, JsonRpcErrorCode.of(StatusCode.TOO_MANY_REQUESTS));
  }

  @Test
  void testOfFallsBackToInternalErrorForProtocolMatches() {
    // BAD_REQUEST matches INVALID_REQUEST, PARSE_ERROR, and INVALID_PARAMS.
    // However, all of these are core protocol errors (protocol=true).
    // The of() method deliberately skips them to avoid leaking protocol errors
    // from generic HTTP exceptions, falling back to INTERNAL_ERROR.
    assertEquals(JsonRpcErrorCode.INTERNAL_ERROR, JsonRpcErrorCode.of(StatusCode.BAD_REQUEST));
  }

  @Test
  void testOfFallsBackToInternalErrorForUnknownCode() {
    // ACCEPTED (202) has no mapping at all in the enum
    assertEquals(JsonRpcErrorCode.INTERNAL_ERROR, JsonRpcErrorCode.of(StatusCode.ACCEPTED));
  }
}
