/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
class McpCompletionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(McpCompletionHandler.class);

  public static McpSchema.CompleteResult handle(
      JoobyMcpServer server, McpSchema.CompleteRequest request) {
    try {
      var identifier = request.ref().identifier();
      var argName = request.argument().name();
      var argValue = request.argument().value();

      Object result = server.invokeCompletion(identifier, argName, argValue);

      return toCompleteResult(result);
    } catch (Exception ex) {
      LOG.error("Error invoking prompt completion '{}':", request.ref().identifier(), ex);
      throw new McpError(
          new McpSchema.JSONRPCResponse.JSONRPCError(INTERNAL_ERROR, ex.getMessage(), null));
    }
  }

  @SuppressWarnings("PMD.NcssCount")
  private static McpSchema.CompleteResult toCompleteResult(Object result) {
    Objects.requireNonNull(result, "Completion result cannot be null");

    if (result instanceof McpSchema.CompleteResult completeResult) {
      return completeResult;
    } else if (result instanceof McpSchema.CompleteResult.CompleteCompletion completion) {
      return new McpSchema.CompleteResult(completion);
    } else if (result instanceof List<?> values) {
      if (values.isEmpty()) {
        return new McpSchema.CompleteResult(
            new McpSchema.CompleteResult.CompleteCompletion(List.of(), 0, false));
      } else {
        var item = values.getFirst();
        if (item instanceof String) {
          //noinspection unchecked
          return new McpSchema.CompleteResult(
              new McpSchema.CompleteResult.CompleteCompletion(
                  (List<String>) values, values.size(), false));
        }
      }
    } else if (result instanceof String singleValue) {
      var completion =
          new McpSchema.CompleteResult.CompleteCompletion(List.of(singleValue), 1, false);
      return new McpSchema.CompleteResult(completion);
    }

    LOG.error("Unsupported completion result type: {}", result.getClass().getName());
    throw new IllegalStateException("Unexpected error occurred while handling completion result");
  }
}
