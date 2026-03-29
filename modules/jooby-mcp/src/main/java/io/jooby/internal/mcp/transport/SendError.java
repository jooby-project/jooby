/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import java.util.List;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.modelcontextprotocol.spec.HttpHeaders;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
class SendError {

  static Context serverIsShuttingDown(Context ctx) {
    ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.INTERNAL_ERROR, "Server is shutting down", null));
    return send(ctx, err);
  }

  static Context invalidAcceptHeader(Context ctx, List<MediaType> acceptedTypes) {
    ctx.setResponseCode(StatusCode.BAD_REQUEST);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.INVALID_REQUEST,
                "Invalid Accept header. Expected: %s".formatted(acceptedTypes),
                null));
    return send(ctx, err);
  }

  static Context missingSessionId(Context ctx) {
    ctx.setResponseCode(StatusCode.BAD_REQUEST);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.INVALID_REQUEST,
                "Session ID required in %s header".formatted(HttpHeaders.MCP_SESSION_ID),
                null));
    return send(ctx, err);
  }

  static Context sessionNotFound(Context ctx, String sessionId) {
    ctx.setResponseCode(StatusCode.NOT_FOUND);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.INVALID_REQUEST,
                "Session %s not found".formatted(sessionId),
                null));
    return send(ctx, err);
  }

  static Context unknownMsgType(Context ctx, String sessionId) {
    ctx.setResponseCode(StatusCode.BAD_REQUEST);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.INVALID_REQUEST,
                "Unknown message type. Session ID: %s".formatted(sessionId),
                null));
    return send(ctx, err);
  }

  static Context msgParseError(Context ctx, String sessionId) {
    ctx.setResponseCode(StatusCode.BAD_REQUEST);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.PARSE_ERROR,
                "Invalid message format. Session ID: %s".formatted(sessionId),
                null));
    return send(ctx, err);
  }

  static Context badRequest(Context ctx, String message) {
    ctx.setResponseCode(StatusCode.BAD_REQUEST);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.INVALID_REQUEST, message, null));
    return send(ctx, err);
  }

  static Context deletionNotAllowed(Context ctx) {
    ctx.setResponseCode(StatusCode.METHOD_NOT_ALLOWED);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.INVALID_REQUEST, "Session deletion is not allowed", null));
    return send(ctx, err);
  }

  static Context internalError(Context ctx, String sessionId) {
    ctx.setResponseCode(StatusCode.SERVER_ERROR);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.INTERNAL_ERROR,
                "Internal Server Error. Session ID: %s".formatted(sessionId),
                null));
    return send(ctx, err);
  }

  static Context internalError(Context ctx) {
    ctx.setResponseCode(StatusCode.SERVER_ERROR);
    var err =
        err(
            new McpSchema.JSONRPCResponse.JSONRPCError(
                McpSchema.ErrorCodes.INTERNAL_ERROR, "Internal Server Error", null));
    return send(ctx, err);
  }

  public static Context error(Context ctx, StatusCode statusCode, int errCode, String msg) {
    ctx.setResponseCode(statusCode);
    var err = err(new McpSchema.JSONRPCResponse.JSONRPCError(errCode, msg, null));
    return send(ctx, err);
  }

  private static McpSchema.JSONRPCResponse err(McpSchema.JSONRPCResponse.JSONRPCError err) {
    return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, null, null, err);
  }

  private static Context send(Context ctx, McpSchema.JSONRPCResponse err) {
    return ctx.accept(MediaType.json) ? ctx.render(err) : ctx.send(err.toString());
  }
}
