/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.modelcontextprotocol.spec.HttpHeaders;
import io.modelcontextprotocol.spec.McpSchema;

@ExtendWith(MockitoExtension.class)
class SendErrorTest {

  @Mock Context ctx;

  @BeforeEach
  void setup() {
    // By default, assume the client accepts JSON to hit the ctx.render() branch
    when(ctx.accept(MediaType.json)).thenReturn(true);
  }

  private void assertErrorResponse(
      StatusCode expectedStatus, int expectedErrorCode, String expectedMessage) {
    verify(ctx).setResponseCode(expectedStatus);

    ArgumentCaptor<McpSchema.JSONRPCResponse> captor =
        ArgumentCaptor.forClass(McpSchema.JSONRPCResponse.class);
    verify(ctx).render(captor.capture());

    McpSchema.JSONRPCResponse response = captor.getValue();
    assertEquals(McpSchema.JSONRPC_VERSION, response.jsonrpc());
    assertNull(response.id());
    assertNull(response.result());

    assertNotNull(response.error());
    assertEquals(expectedErrorCode, response.error().code());
    assertEquals(expectedMessage, response.error().message());
    assertNull(response.error().data());
  }

  @Test
  void testServerIsShuttingDown() {
    SendError.serverIsShuttingDown(ctx);
    assertErrorResponse(
        StatusCode.SERVICE_UNAVAILABLE,
        McpSchema.ErrorCodes.INTERNAL_ERROR,
        "Server is shutting down");
  }

  @Test
  void testInvalidAcceptHeader_WithJsonAccept() {
    List<MediaType> types = List.of(MediaType.json, TransportConstants.TEXT_EVENT_STREAM);
    SendError.invalidAcceptHeader(ctx, types);

    assertErrorResponse(
        StatusCode.BAD_REQUEST,
        McpSchema.ErrorCodes.INVALID_REQUEST,
        "Invalid Accept header. Expected: " + types);
  }

  @Test
  void testInvalidAcceptHeader_WithoutJsonAccept_FallsBackToStringSend() {
    // Force the false branch in the `send` method to ensure 100% branch coverage
    when(ctx.accept(MediaType.json)).thenReturn(false);

    List<MediaType> types = List.of(TransportConstants.TEXT_EVENT_STREAM);
    SendError.invalidAcceptHeader(ctx, types);

    verify(ctx).setResponseCode(StatusCode.BAD_REQUEST);
    // Verifies ctx.send() was used instead of ctx.render()
    verify(ctx).send(anyString());
  }

  @Test
  void testMissingSessionId() {
    SendError.missingSessionId(ctx);
    assertErrorResponse(
        StatusCode.BAD_REQUEST,
        McpSchema.ErrorCodes.INVALID_REQUEST,
        "Session ID required in " + HttpHeaders.MCP_SESSION_ID + " header");
  }

  @Test
  void testSessionNotFound() {
    SendError.sessionNotFound(ctx, "session-123");
    assertErrorResponse(
        StatusCode.NOT_FOUND,
        McpSchema.ErrorCodes.INVALID_REQUEST,
        "Session session-123 not found");
  }

  @Test
  void testUnknownMsgType() {
    SendError.unknownMsgType(ctx, "session-123");
    assertErrorResponse(
        StatusCode.BAD_REQUEST,
        McpSchema.ErrorCodes.INVALID_REQUEST,
        "Unknown message type. Session ID: session-123");
  }

  @Test
  void testMsgParseError() {
    SendError.msgParseError(ctx, "session-123");
    assertErrorResponse(
        StatusCode.BAD_REQUEST,
        McpSchema.ErrorCodes.PARSE_ERROR,
        "Invalid message format. Session ID: session-123");
  }

  @Test
  void testBadRequest() {
    SendError.badRequest(ctx, "Custom bad request reason");
    assertErrorResponse(
        StatusCode.BAD_REQUEST, McpSchema.ErrorCodes.INVALID_REQUEST, "Custom bad request reason");
  }

  @Test
  void testDeletionNotAllowed() {
    SendError.deletionNotAllowed(ctx);
    assertErrorResponse(
        StatusCode.METHOD_NOT_ALLOWED,
        McpSchema.ErrorCodes.INVALID_REQUEST,
        "Session deletion is not allowed");
  }

  @Test
  void testInternalError_WithSessionId() {
    SendError.internalError(ctx, "session-123");
    assertErrorResponse(
        StatusCode.SERVER_ERROR,
        McpSchema.ErrorCodes.INTERNAL_ERROR,
        "Internal Server Error. Session ID: session-123");
  }

  @Test
  void testInternalError_WithoutSessionId() {
    SendError.internalError(ctx);
    assertErrorResponse(
        StatusCode.SERVER_ERROR, McpSchema.ErrorCodes.INTERNAL_ERROR, "Internal Server Error");
  }

  @Test
  void testCustomError() {
    SendError.error(ctx, StatusCode.CONFLICT, -32001, "Custom error state");
    assertErrorResponse(StatusCode.CONFLICT, -32001, "Custom error state");
  }
}
