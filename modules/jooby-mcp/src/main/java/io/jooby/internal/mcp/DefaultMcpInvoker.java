/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import org.slf4j.LoggerFactory;

import io.jooby.Jooby;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.mcp.McpInvoker;
import io.jooby.mcp.McpOperation;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

public class DefaultMcpInvoker implements McpInvoker {
  private final Jooby application;

  public DefaultMcpInvoker(Jooby application) {
    this.application = application;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R invoke(McpOperation operation, SneakyThrows.Supplier<R> action) {
    try {
      return action.get();
    } catch (McpError mcpError) {
      throw mcpError;
    } catch (Throwable cause) {
      var log = LoggerFactory.getLogger(operation.className());
      if (operation.id().startsWith("tools/")) {
        // Tool error
        var errorMessage = cause.getMessage() != null ? cause.getMessage() : cause.toString();
        return (R)
            McpSchema.CallToolResult.builder().addTextContent(errorMessage).isError(true).build();
      }
      var statusCode = application.getRouter().errorCode(cause);
      if (statusCode.value() >= 500) {
        log.error("execution of {} resulted in exception", operation.id(), cause);
      } else {
        log.debug("execution of {} resulted in exception", operation.id(), cause);
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
