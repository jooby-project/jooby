/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import static io.jooby.internal.mcp.McpServerConfig.Transport.STATELESS_STREAMABLE_HTTP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.exception.StartupException;
import io.jooby.internal.mcp.BaseMcpServerRunner;
import io.jooby.internal.mcp.McpServerConfig;
import io.jooby.internal.mcp.McpStatelessServerRunner;
import io.jooby.internal.mcp.McpSyncServerRunner;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;

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

  private static final String MODULE_CONFIG_PREFIX = "mcp";

  private McpJsonMapper mcpJsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
  private final List<JoobyMcpServer> mcpServers = new ArrayList<>();

  public McpModule(JoobyMcpServer joobyMcpServer, JoobyMcpServer... moreMcpServers) {
    mcpServers.add(joobyMcpServer);
    if (moreMcpServers != null) {
      Collections.addAll(mcpServers, moreMcpServers);
    }
  }

  @Override
  public void install(@NonNull Jooby app) {
    Config config = app.getConfig();
    if (!config.hasPath(MODULE_CONFIG_PREFIX)) {
      throw new StartupException("Missing required config path: " + MODULE_CONFIG_PREFIX);
    }

    for (var joobyMcpServer : mcpServers) {
      var serverConfig = resolveServerConfig(config, joobyMcpServer.getServerKey());
      // Load definitions from generated code.
      joobyMcpServer.init(app, mcpJsonMapper);

      // transport + dedicated mcp server
      var runner = buildMcpServerRunner(app, joobyMcpServer, serverConfig);
      runner.run();
      app.getServices().listOf(McpServerConfig.class).add(serverConfig);
    }
  }

  private BaseMcpServerRunner<?> buildMcpServerRunner(
      Jooby app, JoobyMcpServer joobyMcpServer, McpServerConfig serverConfig) {
    var isSingleServer = hasSingleMcpServer();
    if (STATELESS_STREAMABLE_HTTP == serverConfig.getTransport()) {
      return new McpStatelessServerRunner(
          app, joobyMcpServer, serverConfig, mcpJsonMapper, isSingleServer);
    } else {
      return new McpSyncServerRunner(
          app, joobyMcpServer, serverConfig, mcpJsonMapper, isSingleServer);
    }
  }

  private boolean hasSingleMcpServer() {
    return this.mcpServers.size() == 1;
  }

  private McpServerConfig resolveServerConfig(Config config, String serverKey) {
    String path = MODULE_CONFIG_PREFIX + "." + serverKey;
    if (!config.hasPath(path)) {
      throw new StartupException(String.format("Missing required config path: %s", path));
    }
    return McpServerConfig.fromConfig(config.getConfig(path));
  }

  public McpModule mcpJsonMapper(McpJsonMapper mcpJsonMapper) {
    this.mcpJsonMapper = mcpJsonMapper;
    return this;
  }
}
