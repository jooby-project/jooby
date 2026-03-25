/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import java.util.Map;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpSchema;

public abstract class BaseMcpServerRunner<S> {

  protected static final McpTransportContextExtractor<Context> CTX_EXTRACTOR =
      ctx -> {
        var transportContext = Map.<String, Object>of("HEADERS", ctx.headerMap());
        return McpTransportContext.create(transportContext);
      };

  protected final Jooby app;
  protected final JoobyMcpServer joobyMcpServer;
  protected final McpServerConfig serverConfig;
  protected final McpJsonMapper mcpJsonMapper;
  protected final boolean isSingleServer;

  protected final McpToolHandler toolHandler;
  protected final McpResourceHandler resourceHandler;
  protected final McpResourceTemplateHandler resourceTemplateHandler;

  public BaseMcpServerRunner(
      Jooby app,
      JoobyMcpServer joobyMcpServer,
      McpServerConfig serverConfig,
      McpJsonMapper mcpJsonMapper,
      boolean isSingleServer) {
    this.app = app;
    this.joobyMcpServer = joobyMcpServer;
    this.serverConfig = serverConfig;
    this.mcpJsonMapper = mcpJsonMapper;
    this.isSingleServer = isSingleServer;

    this.toolHandler = new McpToolHandler(mcpJsonMapper);
    this.resourceHandler = new McpResourceHandler(mcpJsonMapper);
    this.resourceTemplateHandler = new McpResourceTemplateHandler(mcpJsonMapper);
  }

  public void run() {
    S mcpServer = initMcpServer();

    initTools(mcpServer);
    initPrompts(mcpServer);
    initResources(mcpServer);
    initResourceTemplates(mcpServer);

    addToJoobyRegistry(mcpServer);
    logMcpStart(mcpServer);
    app.onStop(() -> close(mcpServer));
  }

  protected abstract S initMcpServer();

  protected abstract void initTools(S mcpServer);

  protected abstract void initPrompts(S mcpServer);

  protected abstract void initResources(S mcpServer);

  protected abstract void initResourceTemplates(S mcpServer);

  protected abstract void logMcpStart(S mcpServer);

  protected abstract void addToJoobyRegistry(S mcpServer);

  protected abstract void close(S mcpServer);

  protected McpSchema.Tool buildTool(ToolSpec toolSpec) {
    McpSchema.Tool.Builder toolBuilder =
        McpSchema.Tool.builder()
            .name(toolSpec.getName())
            .title(toolSpec.getTitle())
            .description(toolSpec.getDescription())
            .inputSchema(mcpJsonMapper, toolSpec.getInputSchema());

    if (toolSpec.getOutputSchema() != null) {
      toolBuilder.outputSchema(mcpJsonMapper, toolSpec.getOutputSchema());
    }

    if (toolSpec.getAnnotations() != null) {
      toolBuilder.annotations(toolSpec.getAnnotations());
    }

    return toolBuilder.build();
  }

  @SuppressWarnings("PMD.NPathComplexity")
  protected McpSchema.ServerCapabilities computeCapabilities() {
    var builder = McpSchema.ServerCapabilities.builder();

    if (!joobyMcpServer.getTools().isEmpty()) {
      builder.tools(true);
    }

    if (!joobyMcpServer.getPrompts().isEmpty()) {
      builder.prompts(true);
    }

    if (!joobyMcpServer.getCompletions().isEmpty()) {
      builder.completions();
    }

    if (!joobyMcpServer.getResources().isEmpty()) {
      builder.resources(true, true);
    }

    return builder.build();
  }
}
