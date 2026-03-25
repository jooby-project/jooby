/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
class McpResourceHandler {

  private static final Logger LOG = LoggerFactory.getLogger(McpResourceHandler.class);

  private final McpJsonMapper mcpJsonMapper;

  public McpResourceHandler(McpJsonMapper mcpJsonMapper) {
    this.mcpJsonMapper = mcpJsonMapper;
  }

  public McpSchema.ReadResourceResult handle(
      JoobyMcpServer server, McpSchema.ReadResourceRequest request) {
    var uri = request.uri();

    try {
      Object result = server.readResource(uri);
      return toResourceResult(result, uri, mcpJsonMapper);
    } catch (Exception ex) {
      LOG.error("Error reading resource by URI '{}':", uri, ex);
      throw new McpError(
          new McpSchema.JSONRPCResponse.JSONRPCError(INTERNAL_ERROR, ex.getMessage(), null));
    }
  }

  static McpSchema.ReadResourceResult toResourceResult(
      Object result, String uri, McpJsonMapper mcpJsonMapper) throws IOException {
    if (result == null) {
      return new McpSchema.ReadResourceResult(List.of());
    } else if (result instanceof McpSchema.ReadResourceResult resourceResult) {
      return resourceResult;
    } else if (result instanceof McpSchema.ResourceContents resourceContents) {
      return new McpSchema.ReadResourceResult(List.of(resourceContents));
    } else if (result instanceof List<?> contents) {
      return handleListReturnType(result, uri, mcpJsonMapper, contents);
    } else {
      return toJsonResult(result, uri, mcpJsonMapper);
    }
  }

  private static McpSchema.ReadResourceResult handleListReturnType(
      Object result, String uri, McpJsonMapper mcpJsonMapper, List<?> contents) throws IOException {
    if (contents.isEmpty()) {
      return new McpSchema.ReadResourceResult(List.of());
    } else {
      var item = contents.getFirst();
      if (item instanceof McpSchema.ResourceContents) {
        //noinspection unchecked
        return new McpSchema.ReadResourceResult((List<McpSchema.ResourceContents>) contents);
      } else {
        return toJsonResult(result, uri, mcpJsonMapper);
      }
    }
  }

  static McpSchema.ReadResourceResult toJsonResult(
      Object result, String uri, McpJsonMapper mcpJsonMapper) throws IOException {
    var resultStr = mcpJsonMapper.writeValueAsString(result);
    var content = new McpSchema.TextResourceContents(uri, "application/json", resultStr);
    return new McpSchema.ReadResourceResult(List.of(content));
  }
}
