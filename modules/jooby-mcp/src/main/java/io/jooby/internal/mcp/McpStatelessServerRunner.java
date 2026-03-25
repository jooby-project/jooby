/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.mcp.JoobyMcpServer;
import io.jooby.mcp.transport.JoobyStatelessServerTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
public class McpStatelessServerRunner extends BaseMcpServerRunner<McpStatelessSyncServer> {

  private static final Logger LOG = LoggerFactory.getLogger(McpStatelessServerRunner.class);

  public McpStatelessServerRunner(
      Jooby app,
      JoobyMcpServer joobyMcpServer,
      McpServerConfig serverConfig,
      McpJsonMapper mcpJsonMapper,
      boolean isSingleServer) {
    super(app, joobyMcpServer, serverConfig, mcpJsonMapper, isSingleServer);
  }

  @Override
  protected McpStatelessSyncServer initMcpServer() {
    List<McpStatelessServerFeatures.SyncCompletionSpecification> completions = initCompletions();

    var transportProvider =
        new JoobyStatelessServerTransport(app, mcpJsonMapper, serverConfig, CTX_EXTRACTOR);
    return McpServer.sync(transportProvider)
        .serverInfo(serverConfig.getName(), serverConfig.getVersion())
        .capabilities(computeCapabilities())
        .completions(completions)
        .instructions(serverConfig.getInstructions())
        .build();
  }

  private List<McpStatelessServerFeatures.SyncCompletionSpecification> initCompletions() {
    List<McpStatelessServerFeatures.SyncCompletionSpecification> completions = new ArrayList<>();
    for (McpSchema.CompleteReference ref : joobyMcpServer.getCompletions()) {
      var completion =
          new McpStatelessServerFeatures.SyncCompletionSpecification(
              ref, (ctx, request) -> McpCompletionHandler.handle(joobyMcpServer, request));
      completions.add(completion);
    }
    return completions;
  }

  @Override
  protected void initTools(McpStatelessSyncServer mcpServer) {
    for (Map.Entry<String, ToolSpec> entry : joobyMcpServer.getTools().entrySet()) {
      ToolSpec toolSpec = entry.getValue();
      var syncToolSpec =
          new McpStatelessServerFeatures.SyncToolSpecification.Builder()
              .tool(buildTool(toolSpec))
              .callHandler((ctx, request) -> toolHandler.handle(request, joobyMcpServer, null))
              .build();

      mcpServer.addTool(syncToolSpec);
    }
  }

  @Override
  protected void initPrompts(McpStatelessSyncServer mcpServer) {
    for (Map.Entry<String, McpSchema.Prompt> entry : joobyMcpServer.getPrompts().entrySet()) {
      mcpServer.addPrompt(
          new McpStatelessServerFeatures.SyncPromptSpecification(
              entry.getValue(),
              (ctx, request) -> McpPromptHandler.handle(joobyMcpServer, request, null)));
    }
  }

  @Override
  protected void initResources(McpStatelessSyncServer mcpServer) {
    for (McpSchema.Resource resource : joobyMcpServer.getResources()) {
      mcpServer.addResource(
          new McpStatelessServerFeatures.SyncResourceSpecification(
              resource, (ctx, request) -> resourceHandler.handle(joobyMcpServer, request)));
    }
  }

  @Override
  protected void initResourceTemplates(McpStatelessSyncServer mcpServer) {
    for (McpSchema.ResourceTemplate template : joobyMcpServer.getResourceTemplates()) {
      var syncTemplateSpec =
          new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
              template,
              (ctx, request) -> resourceTemplateHandler.handle(joobyMcpServer, template, request));
      mcpServer.addResourceTemplate(syncTemplateSpec);
    }
  }

  @Override
  protected void addToJoobyRegistry(McpStatelessSyncServer mcpServer) {
    var registry = app.getServices();
    if (isSingleServer) {
      registry.put(McpStatelessSyncServer.class, mcpServer);
    } else {
      var serviceKey = ServiceKey.key(McpStatelessSyncServer.class, joobyMcpServer.getServerKey());
      registry.put(serviceKey, mcpServer);
    }
  }

  @Override
  protected void close(McpStatelessSyncServer mcpServer) {
    mcpServer.close();
  }

  @Override
  protected void logMcpStart(McpStatelessSyncServer mcpServer) {
    LOG.info(
        """

        MCP server started with:
            name: {}
            version: {}
            transport: {}
            capabilities: {}
        """,
        mcpServer.getServerInfo().name(),
        mcpServer.getServerInfo().version(),
        serverConfig.getTransport().getValue(),
        mcpServer.getServerCapabilities());
  }
}
