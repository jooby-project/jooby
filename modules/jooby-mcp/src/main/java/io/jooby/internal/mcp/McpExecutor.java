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
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.mcp.McpChain;
import io.jooby.mcp.McpInvoker;
import io.jooby.mcp.McpOperation;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

public class McpExecutor implements McpInvoker {
  private final Jooby application;

  public McpExecutor(Jooby application) {
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
    } catch (Throwable cause) {
      operation.exception(cause);
      log(operation, cause);
      if (SneakyThrows.isFatal(cause)) {
        throw SneakyThrows.propagate(cause);
      }
      var code = toMcpErrorCode(cause);
      if (operation.isTool()) {
        // Tool error
        var errorMessage =
            cause.getMessage() != null ? cause.getMessage() : "Unknown error occurred";
        var textContent = new McpSchema.TextContent(errorMessage);
        return McpSchema.CallToolResult.builder().addContent(textContent).isError(true).build();
      }
      if (cause instanceof McpError mcpError) {
        throw mcpError;
      } else {
        throw new McpError(
            new McpSchema.JSONRPCResponse.JSONRPCError(code, cause.getMessage(), null));
      }
    }
  }

  private void log(McpOperation operation, Throwable cause) {
    var log = LoggerFactory.getLogger(operation.getClassName());
    var code = toMcpErrorCode(cause);
    if (isServerError(code)) {
      log.error("execution of {} resulted in exception", operation.getId(), cause);
    } else {
      log.debug("execution of {} resulted in exception", operation.getId(), cause);
    }
  }

  static boolean isServerError(int code) {
    // -32603 is Internal Error. Custom server errors usually fall outside the -32600 to -32699
    // reserved range.
    return code == McpSchema.ErrorCodes.INTERNAL_ERROR || code < -32700;
  }

  private int toMcpErrorCode(Throwable cause) {
    if (cause instanceof McpError mcpError && mcpError.getJsonRpcError() != null) {
      return mcpError.getJsonRpcError().code();
    }
    var statusCode = application.getRouter().errorCode(cause);
    return switch (statusCode.value()) {
      case StatusCode.BAD_REQUEST_CODE, StatusCode.CONFLICT_CODE ->
          McpSchema.ErrorCodes.INVALID_PARAMS;
      case StatusCode.NOT_FOUND_CODE -> McpSchema.ErrorCodes.RESOURCE_NOT_FOUND;

      default -> McpSchema.ErrorCodes.INTERNAL_ERROR;
    };
  }
}
