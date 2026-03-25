/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INVALID_PARAMS;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
public class McpToolHandler {

  private static final Logger LOG = LoggerFactory.getLogger(McpToolHandler.class);

  private final McpJsonMapper mcpJsonMapper;

  public McpToolHandler(McpJsonMapper mcpJsonMapper) {
    this.mcpJsonMapper = mcpJsonMapper;
  }

  public McpSchema.CallToolResult handle(
      McpSchema.CallToolRequest request, JoobyMcpServer server, McpSyncServerExchange exchange) {
    String toolName = request.name();
    ToolSpec toolSpec = server.getTools().get(toolName);
    if (toolSpec == null) {
      throwUnknownToolErr(toolName);
    }
    try {
      verifyRequiredArguments(request.arguments(), toolSpec.getRequiredArguments());

      Object result = server.invokeTool(toolName, request.arguments(), exchange);
      return toCallToolResult(toolSpec, result);
    } catch (Exception ex) {
      LOG.error("Error invoking tool '{}':", toolName, ex);
      return buildTextResult(ex.getMessage(), true);
    }
  }

  private McpSchema.CallToolResult toCallToolResult(ToolSpec spec, Object result)
      throws IOException {
    var hasOutputSchema = spec.getOutputSchema() != null;
    if (result == null) {
      return buildTextResult("null", false);
    } else if (result instanceof McpSchema.CallToolResult callToolResult) {
      return callToolResult;
    } else if (result instanceof String str) {
      return buildTextResult(str, false);
    } else if (result instanceof McpSchema.Content content) {
      return McpSchema.CallToolResult.builder().content(List.of(content)).isError(false).build();
    } else {
      if (hasOutputSchema) {
        return McpSchema.CallToolResult.builder().structuredContent(result).isError(false).build();
      } else {
        var resultStr = mcpJsonMapper.writeValueAsString(result);
        return buildTextResult(resultStr, false);
      }
    }
  }

  private void verifyRequiredArguments(
      Map<String, Object> actualArguments, List<String> requiredArguments) {
    for (String requiredArg : requiredArguments) {
      var argument = actualArguments.get(requiredArg);
      if (argument == null) {
        throw new IllegalArgumentException("Missing required argument: " + requiredArg);
      }

      if (argument instanceof String str && str.isEmpty()) {
        throw new IllegalArgumentException("Required argument is empty: " + requiredArg);
      }
    }
  }

  private McpSchema.CallToolResult buildTextResult(String text, boolean isError) {
    return McpSchema.CallToolResult.builder().addTextContent(text).isError(isError).build();
  }

  private static void throwUnknownToolErr(String toolName) {
    throw new McpError(
        new McpSchema.JSONRPCResponse.JSONRPCError(
            INVALID_PARAMS,
            "Unknown tool '" + toolName + "'. Please verify such a tool is registered.",
            null));
  }
}
