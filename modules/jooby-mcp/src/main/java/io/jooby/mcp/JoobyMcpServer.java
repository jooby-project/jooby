/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import java.util.List;
import java.util.Map;

import io.jooby.Jooby;
import io.jooby.internal.mcp.ToolSpec;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
public interface JoobyMcpServer {

  String getServerKey();

  void init(Jooby app, McpJsonMapper mcpJsonMapper);

  Object invokeTool(String toolName, Map<String, Object> args, McpSyncServerExchange exchange);

  Object invokePrompt(String promptName, Map<String, Object> args, McpSyncServerExchange exchange);

  Object invokeCompletion(String identifier, String argumentName, String input);

  Object readResource(String uri);

  Object readResourceByTemplate(String uri, Map<String, Object> templateArgs);

  Map<String, ToolSpec> getTools();

  Map<String, McpSchema.Prompt> getPrompts();

  List<McpSchema.Resource> getResources();

  List<McpSchema.ResourceTemplate> getResourceTemplates();

  List<McpSchema.CompleteReference> getCompletions();
}
