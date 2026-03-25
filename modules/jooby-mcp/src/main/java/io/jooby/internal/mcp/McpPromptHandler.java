/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;
import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INVALID_PARAMS;
import static io.modelcontextprotocol.spec.McpSchema.Role.USER;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
class McpPromptHandler {

  private static final Logger LOG = LoggerFactory.getLogger(McpPromptHandler.class);

  public static McpSchema.GetPromptResult handle(
      JoobyMcpServer server, McpSchema.GetPromptRequest request, McpSyncServerExchange exchange) {
    var promptName = request.name();
    if (!server.getPrompts().containsKey(promptName)) {
      throwUnknownPromptErr(promptName);
    }

    try {
      Object result = server.invokePrompt(promptName, request.arguments(), exchange);
      return toPromptResult(result);
    } catch (Exception ex) {
      LOG.error("Error invoking prompt '{}':", request.name(), ex);
      throw new McpError(
          new McpSchema.JSONRPCResponse.JSONRPCError(INTERNAL_ERROR, ex.getMessage(), null));
    }
  }

  @SuppressWarnings("PMD.NcssCount")
  private static McpSchema.GetPromptResult toPromptResult(Object result) {
    if (result == null) {
      return new McpSchema.GetPromptResult(null, List.of());
    } else if (result instanceof McpSchema.GetPromptResult promptResult) {
      return promptResult;
    } else if (result instanceof McpSchema.PromptMessage promptMessage) {
      return new McpSchema.GetPromptResult(null, List.of(promptMessage));
    } else if (result instanceof McpSchema.Content content) {
      var promptMessage = new McpSchema.PromptMessage(USER, content);
      return new McpSchema.GetPromptResult(null, List.of(promptMessage));
    } else if (result instanceof String str) {
      var promptMessage = new McpSchema.PromptMessage(USER, new McpSchema.TextContent(str));
      return new McpSchema.GetPromptResult(null, List.of(promptMessage));
    } else if (result instanceof List<?> items) {
      //noinspection unchecked
      return handleListReturnType((List<McpSchema.PromptMessage>) result, items);
    } else {
      var promptMessage =
          new McpSchema.PromptMessage(USER, new McpSchema.TextContent(result.toString()));
      return new McpSchema.GetPromptResult(null, List.of(promptMessage));
    }
  }

  private static McpSchema.GetPromptResult handleListReturnType(
      List<McpSchema.PromptMessage> result, List<?> items) {
    if (items.isEmpty()) {
      return new McpSchema.GetPromptResult(null, List.of());
    } else {
      var item = items.getFirst();
      if (item instanceof McpSchema.PromptMessage) {
        return new McpSchema.GetPromptResult(null, result);
      } else {
        var msgs =
            items.stream()
                .map(
                    i -> new McpSchema.PromptMessage(USER, new McpSchema.TextContent(i.toString())))
                .toList();
        return new McpSchema.GetPromptResult(null, msgs);
      }
    }
  }

  private static void throwUnknownPromptErr(String promptName) {
    throw new McpError(
        new McpSchema.JSONRPCResponse.JSONRPCError(
            INVALID_PARAMS,
            "Unknown prompt name '" + promptName + "'. Please verify such a prompt is registered.",
            null));
  }
}
