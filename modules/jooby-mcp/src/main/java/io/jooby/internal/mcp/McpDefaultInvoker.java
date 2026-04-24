/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.mcp.McpChain;
import io.jooby.mcp.McpInvoker;
import io.jooby.mcp.McpOperation;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

public class McpDefaultInvoker implements McpInvoker {
  private final Jooby application;

  public McpDefaultInvoker(Jooby application) {
    this.application = application;
  }

  @SuppressWarnings("unchecked")
  public @NonNull Object invoke(
      @Nullable McpSyncServerExchange exchange,
      @NonNull McpTransportContext transportContext,
      @NonNull McpOperation operation,
      @NonNull McpChain next) {
    try {
      return next.proceed(exchange, transportContext, operation);
    } catch (McpError mcpError) {
      throw mcpError;
    } catch (Throwable cause) {
      var log = LoggerFactory.getLogger(operation.getClassName());
      if (operation.isTool()) {
        // Tool error
        var errorMessage = cause.getMessage() != null ? cause.getMessage() : cause.toString();
        return McpSchema.CallToolResult.builder()
            .addTextContent(errorMessage)
            .isError(true)
            .build();
      }
      var statusCode = application.getRouter().errorCode(cause);
      if (statusCode.value() >= 500) {
        log.error("execution of {} resulted in exception", operation.getId(), cause);
      } else {
        log.debug("execution of {} resulted in exception", operation.getId(), cause);
      }
      var mcpErrorCode = toMcpErrorCode(statusCode);
      throw new McpError(
          new McpSchema.JSONRPCResponse.JSONRPCError(mcpErrorCode, cause.getMessage(), null));
    }
  }

  private int toMcpErrorCode(StatusCode statusCode) {
    return switch (statusCode.value()) {
      case StatusCode.BAD_REQUEST_CODE, StatusCode.CONFLICT_CODE ->
          McpSchema.ErrorCodes.INVALID_PARAMS;
      case StatusCode.NOT_FOUND_CODE -> McpSchema.ErrorCodes.RESOURCE_NOT_FOUND;

      default -> McpSchema.ErrorCodes.INTERNAL_ERROR;
    };
  }
}
