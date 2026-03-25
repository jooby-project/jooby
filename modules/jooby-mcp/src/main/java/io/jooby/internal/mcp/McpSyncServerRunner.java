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
import io.jooby.mcp.transport.JoobySseTransportProvider;
import io.jooby.mcp.transport.JoobyStreamableServerTransportProvider;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
public class McpSyncServerRunner extends BaseMcpServerRunner<McpSyncServer> {

  private static final Logger LOG = LoggerFactory.getLogger(McpSyncServerRunner.class);

  public McpSyncServerRunner(
      Jooby app,
      JoobyMcpServer joobyMcpServer,
      McpServerConfig serverConfig,
      McpJsonMapper mcpJsonMapper,
      boolean isSingleServer) {
    super(app, joobyMcpServer, serverConfig, mcpJsonMapper, isSingleServer);
  }

  @Override
  protected McpSyncServer initMcpServer() {
    List<McpServerFeatures.SyncCompletionSpecification> completions = initCompletions();

    if (McpServerConfig.Transport.SSE == serverConfig.getTransport()) {
      var transportProvider = new JoobySseTransportProvider(app, serverConfig, mcpJsonMapper);
      return McpServer.sync(transportProvider)
          .serverInfo(serverConfig.getName(), serverConfig.getVersion())
          .capabilities(computeCapabilities())
          .completions(completions)
          .instructions(serverConfig.getInstructions())
          .build();
    } else if (McpServerConfig.Transport.STREAMABLE_HTTP == serverConfig.getTransport()) {
      var transportProvider =
          new JoobyStreamableServerTransportProvider(
              app, mcpJsonMapper, serverConfig, CTX_EXTRACTOR);

      return McpServer.sync(transportProvider)
          .serverInfo(serverConfig.getName(), serverConfig.getVersion())
          .capabilities(computeCapabilities())
          .completions(completions)
          .instructions(serverConfig.getInstructions())
          .build();
    } else {
      throw new IllegalStateException("Unsupported transport: " + serverConfig.getTransport());
    }
  }

  private List<McpServerFeatures.SyncCompletionSpecification> initCompletions() {
    List<McpServerFeatures.SyncCompletionSpecification> completions = new ArrayList<>();
    for (McpSchema.CompleteReference ref : joobyMcpServer.getCompletions()) {
      var completion =
          new McpServerFeatures.SyncCompletionSpecification(
              ref, (exchange, request) -> McpCompletionHandler.handle(joobyMcpServer, request));
      completions.add(completion);
    }
    return completions;
  }

  @Override
  protected void initTools(McpSyncServer mcpServer) {
    for (Map.Entry<String, ToolSpec> entry : joobyMcpServer.getTools().entrySet()) {
      ToolSpec toolSpec = entry.getValue();

      var syncToolSpec =
          new McpServerFeatures.SyncToolSpecification.Builder()
              .tool(buildTool(toolSpec))
              .callHandler(
                  (exchange, request) -> toolHandler.handle(request, joobyMcpServer, exchange))
              .build();

      mcpServer.addTool(syncToolSpec);
    }
  }

  @Override
  protected void initPrompts(McpSyncServer mcpServer) {
    for (Map.Entry<String, McpSchema.Prompt> entry : joobyMcpServer.getPrompts().entrySet()) {
      mcpServer.addPrompt(
          new McpServerFeatures.SyncPromptSpecification(
              entry.getValue(),
              (exchange, request) -> McpPromptHandler.handle(joobyMcpServer, request, exchange)));
    }
  }

  @Override
  protected void initResources(McpSyncServer mcpServer) {
    for (McpSchema.Resource resource : joobyMcpServer.getResources()) {
      mcpServer.addResource(
          new McpServerFeatures.SyncResourceSpecification(
              resource, (exchange, request) -> resourceHandler.handle(joobyMcpServer, request)));
    }
  }

  @Override
  protected void initResourceTemplates(McpSyncServer mcpServer) {
    for (McpSchema.ResourceTemplate template : joobyMcpServer.getResourceTemplates()) {
      var syncTemplateSpec =
          new McpServerFeatures.SyncResourceTemplateSpecification(
              template,
              (exchange, request) ->
                  resourceTemplateHandler.handle(joobyMcpServer, template, request));
      mcpServer.addResourceTemplate(syncTemplateSpec);
    }
  }

  @Override
  protected void addToJoobyRegistry(McpSyncServer mcpServer) {
    var registry = app.getServices();
    if (isSingleServer) {
      registry.put(McpSyncServer.class, mcpServer);
    } else {
      var serviceKey = ServiceKey.key(McpSyncServer.class, joobyMcpServer.getServerKey());
      registry.put(serviceKey, mcpServer);
    }
  }

  @Override
  protected void close(McpSyncServer mcpServer) {
    mcpServer.close();
  }

  @Override
  protected void logMcpStart(McpSyncServer mcpServer) {
    LOG.info(
        """

        MCP server started with:
            name: {}
            version: {}
            transport: {}
            keepAliveInterval: {}
            disallowDelete: {}
            capabilities: {}
        """,
        mcpServer.getServerInfo().name(),
        mcpServer.getServerInfo().version(),
        serverConfig.getTransport().getValue(),
        serverConfig.getKeepAliveInterval() == null
            ? "N/A"
            : serverConfig.getKeepAliveInterval() + " s",
        serverConfig.isDisallowDelete(),
        mcpServer.getServerCapabilities());
  }
}
