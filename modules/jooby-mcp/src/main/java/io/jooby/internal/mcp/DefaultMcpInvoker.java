/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import io.jooby.Jooby;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.mcp.McpInvoker;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

public class DefaultMcpInvoker implements McpInvoker {
  private final Jooby application;

  public DefaultMcpInvoker(Jooby application) {
    this.application = application;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R invoke(String operationId, SneakyThrows.Supplier<R> action) {
    try {
      return action.get();
    } catch (McpError mcpError) {
      throw mcpError;
    } catch (Throwable cause) {
      if (operationId.startsWith("tools/")) {
        // Tool error
        var errorMessage = cause.getMessage() != null ? cause.getMessage() : cause.toString();
        return (R)
            McpSchema.CallToolResult.builder().addTextContent(errorMessage).isError(true).build();
      }
      var statusCode = application.getRouter().errorCode(cause);
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
