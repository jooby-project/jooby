/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.mcp.JoobyMcpServer;
import io.jooby.mcp.ResourceUri;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.DefaultMcpUriTemplateManager;

/**
 * @author kliushnichenko
 */
class McpResourceTemplateHandler {

  private static final Logger LOG = LoggerFactory.getLogger(McpResourceTemplateHandler.class);

  private final McpJsonMapper mcpJsonMapper;

  public McpResourceTemplateHandler(McpJsonMapper mcpJsonMapper) {
    this.mcpJsonMapper = mcpJsonMapper;
  }

  public McpSchema.ReadResourceResult handle(
      JoobyMcpServer server,
      McpSchema.ResourceTemplate resourceTemplate,
      McpSchema.ReadResourceRequest request) {
    var uri = request.uri();
    var uriTemplate = resourceTemplate.uriTemplate();
    DefaultMcpUriTemplateManager manager = new DefaultMcpUriTemplateManager(uriTemplate);

    Map<String, Object> args = new HashMap<>();
    args.put(ResourceUri.CTX_KEY, uri);
    args.putAll(manager.extractVariableValues(uri));

    try {
      Object result = server.readResourceByTemplate(uriTemplate, args);
      return McpResourceHandler.toResourceResult(result, uri, mcpJsonMapper);
    } catch (Exception ex) {
      LOG.error("Error reading resource template by URI '{}':", uri, ex);
      throw new McpError(
          new McpSchema.JSONRPCResponse.JSONRPCError(INTERNAL_ERROR, ex.getMessage(), null));
    }
  }
}
