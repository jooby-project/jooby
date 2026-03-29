/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import static io.jooby.SneakyThrows.throwingConsumer;
import static io.jooby.mcp.McpModule.Transport.STATELESS_STREAMABLE_HTTP;
import static io.jooby.mcp.McpModule.Transport.STREAMABLE_HTTP;

import java.util.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.exception.StartupException;
import io.jooby.internal.mcp.McpServerConfig;
import io.jooby.mcp.transport.SseTransportProvider;
import io.jooby.mcp.transport.StatelessTransportProvider;
import io.jooby.mcp.transport.StreamableTransportProvider;
import io.jooby.mcp.transport.WebSocketTransportProvider;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.*;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP (Model Context Protocol) module for Jooby.
 *
 * <p>The MCP module provides integration with the Model Context Protocol server, enabling
 * standardized communication between clients and servers. It allows applications to:
 *
 * <ul>
 *   <li>Expose server capabilities as tools, resources, and prompts
 *   <li>Handle client connections and sessions via SSE
 *   <li>Process protocol messages and events
 *   <li>Manage server capabilities and tool specifications
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <p>Add the module to your application:
 *
 * <pre>{@code
 * {
 *   install(new JacksonModule());
 *   install(new McpModule(new DefaultMcpServer()));
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>The module requires the following configuration in your application.conf:
 *
 * <pre>{@code
 * mcp.default {
 *     name: "my-awesome-mcp-server"     # Required
 *     version: "0.0.1"                  # Required
 *     sseEndpoint: "/mcp/sse"           # Optional (default: /mcp/sse)
 *     messageEndpoint: "/mcp/message"   # Optional (default: /mcp/message)
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>MCP server implementation with SSE transport
 *   <li>Tools Auto-discovery at build time
 *   <li>Server capabilities configuration
 *   <li>Configurable endpoints
 *   <li>Multiple servers support
 * </ul>
 *
 * <h2>Multiple servers</h2>
 *
 * <p>To run multiple MCP server instances in the same application, use a @McpServer("calculator")
 * annotation:
 *
 * <pre>{@code
 * {
 *
 *   install(new JacksonModule());
 *   install(new McpModule(new DefaultMcpServer(), new CalculatorMcpServer()));
 * }
 * }</pre>
 *
 * <p>Each instance requires its own configuration block:
 *
 * <pre>{@code
 * mcp {
 *  default {
 *    name: "default-mcp-server"
 *    version: "1.0.0"
 *    sseEndpoint: "/mcp/sse"
 *    messageEndpoint: "/mcp/message"
 *  }
 *  calculator {
 *    name: "calculator-mcp-server"
 *    version: "1.0.0"
 *    sseEndpoint: "/mcp/calculator/sse"
 *    messageEndpoint: "/mcp/calculator/message"
 *  }
 * }
 *
 * }</pre>
 *
 * @author kliushnichenko
 * @since 1.0.0
 */
public class McpModule implements Extension {

  protected static final McpTransportContextExtractor<Context> CTX_EXTRACTOR =
      ctx -> {
        var transportContext = Map.<String, Object>of("HEADERS", ctx.headerMap());
        return McpTransportContext.create(transportContext);
      };

  private static final String MODULE_CONFIG_PREFIX = "mcp";

  private Transport defaultTransport = STREAMABLE_HTTP;

  private final List<McpService> mcpServices = new ArrayList<>();

  public McpModule(McpService mcpService, McpService... mcpServices) {
    this.mcpServices.add(mcpService);
    if (mcpServices != null) {
      Collections.addAll(this.mcpServices, mcpServices);
    }
  }

  public McpModule transport(@NonNull Transport transport) {
    this.defaultTransport = transport;
    return this;
  }

  @Override
  public void install(@NonNull Jooby app) {
    var services = app.getServices();
    var mcpJsonMapper = services.require(McpJsonMapper.class);
    // Group services by server
    var mcpServiceMap = new HashMap<String, List<McpService>>();
    for (var mcpService : mcpServices) {
      var serverKey = Optional.ofNullable(mcpService.serverKey()).orElse("default");
      mcpServiceMap.computeIfAbsent(serverKey, k -> new ArrayList<>()).add(mcpService);
    }
    // Boot everything
    for (var serverEntry : mcpServiceMap.entrySet()) {
      var mcpConfig = mcpServerConfig(app, serverEntry.getKey());
      var capabilities = new McpSchema.ServerCapabilities.Builder();
      serverEntry.getValue().forEach(it -> it.capabilities(capabilities));

      if (mcpConfig.getTransport() == STATELESS_STREAMABLE_HTTP) {
        var transport =
            new StatelessTransportProvider(app, mcpJsonMapper, mcpConfig, CTX_EXTRACTOR);
        var statelessServer =
            McpServer.sync(transport)
                .serverInfo(mcpConfig.getName(), mcpConfig.getVersion())
                .completions(statelessCompletions(serverEntry))
                .capabilities(capabilities.build())
                .instructions(mcpConfig.getInstructions())
                .build();
        serverEntry
            .getValue()
            .forEach(throwingConsumer(service -> service.install(app, statelessServer)));
        app.onStop(statelessServer::close);
      } else {
        // Stupid MCP types, but it's the only way to make it work.
        var syncServer =
            (switch (mcpConfig.getTransport()) {
                  case STREAMABLE_HTTP ->
                      McpServer.sync(
                          new StreamableTransportProvider(
                              app, mcpJsonMapper, mcpConfig, CTX_EXTRACTOR));
                  case SSE ->
                      McpServer.sync(
                          new SseTransportProvider(app, mcpConfig, mcpJsonMapper, CTX_EXTRACTOR));
                  case WEBSOCKET ->
                      McpServer.sync(
                          new WebSocketTransportProvider(
                              app, mcpConfig, mcpJsonMapper, CTX_EXTRACTOR));
                  default ->
                      throw new IllegalStateException(
                          "Unsupported transport: " + mcpConfig.getTransport());
                })
                .serverInfo(mcpConfig.getName(), mcpConfig.getVersion())
                .completions(completions(serverEntry))
                .capabilities(capabilities.build())
                .instructions(mcpConfig.getInstructions())
                .build();
        serverEntry
            .getValue()
            .forEach(throwingConsumer(service -> service.install(app, syncServer)));
        app.onStop(syncServer::close);
      }
    }
  }

  private static List<McpServerFeatures.SyncCompletionSpecification> completions(
      Map.Entry<String, List<McpService>> serverEntry) {
    return serverEntry.getValue().stream()
        .map(McpService::completions)
        .flatMap(List::stream)
        .toList();
  }

  private static List<McpStatelessServerFeatures.SyncCompletionSpecification> statelessCompletions(
      Map.Entry<String, List<McpService>> serverEntry) {
    return serverEntry.getValue().stream()
        .map(McpService::statelessCompletions)
        .flatMap(List::stream)
        .toList();
  }

  private McpServerConfig mcpServerConfig(Jooby application, String key) {
    var config = application.getConfig();
    var mcpPath = MODULE_CONFIG_PREFIX + "." + key;
    if (config.hasPath(mcpPath)) {
      return McpServerConfig.fromConfig(key, config.getConfig(mcpPath));
    } else if (key.equals("default")) {
      var defaults = new McpServerConfig(application.getName(), application.getVersion());
      defaults.setTransport(defaultTransport);
      defaults.setSseEndpoint(McpServerConfig.DEFAULT_SSE_ENDPOINT);
      defaults.setMessageEndpoint(McpServerConfig.DEFAULT_MESSAGE_ENDPOINT);
      defaults.setMcpEndpoint(McpServerConfig.DEFAULT_MCP_ENDPOINT);
      return defaults;
    } else {
      throw new StartupException("Missing MCP server configuration: " + mcpPath);
    }
  }

  public enum Transport {
    SSE("sse"),
    STREAMABLE_HTTP("streamable-http"),
    STATELESS_STREAMABLE_HTTP("stateless-streamable-http"),
    WEBSOCKET("websocket");

    private final String value;

    Transport(String value) {
      this.value = value;
    }

    public static Transport of(String value) {
      for (Transport transport : values()) {
        if (transport.value.equalsIgnoreCase(value)) {
          return transport;
        }
      }
      throw new IllegalArgumentException("Unknown transport value: " + value);
    }

    public String getValue() {
      return value;
    }
  }
}
