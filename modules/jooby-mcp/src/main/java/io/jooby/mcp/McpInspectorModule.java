/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import java.util.List;

import io.jooby.*;
import io.jooby.exception.RegistryException;
import io.jooby.exception.StartupException;
import io.jooby.internal.mcp.McpServerConfig;

/**
 * MCP Inspector module for Jooby.
 *
 * <p>The MCP Inspector module provides a web-based interface for inspecting and interacting with
 * local MCP server running on the same app. It serves a frontend application that allows users to
 * connect to MCP servers, view their capabilities, and test various protocol features.
 *
 * <h2>Usage</h2>
 *
 * <p>Add the module to your application:
 *
 * <pre>{@code
 * {
 *   install(new McpInspectorModule());
 * }
 * }</pre>
 *
 * All available configurations example:
 *
 * <pre>{@code
 * {
 *   install(new McpInspectorModule()
 *      .path("/inspector")               // Optional, default is /mcp-inspector
 *      .autoConnect(false)               // Optional, default is true
 *      .defaultServer("my-mcp-server")   // Optional, default is the first configured MCP server
 *   );
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>The module requires at least one MCP server to be configured in your Jooby application.
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>Serves a web-based MCP Inspector UI
 *   <li>Automatically configures the inspector to connect to the local MCP server with respect to
 *       transport and endpoint
 *   <li>Supports only direct connection and enables it automatically when the page loads
 * </ul>
 *
 * @author kliushnichenko
 * @since 4.2.0
 */
public class McpInspectorModule implements Extension {

  private static final String DIST =
      "https://cdn.jsdelivr.net/npm/@modelcontextprotocol/inspector-client@0.20.0/dist";

  private static final String INDEX_HTML_TEMPLATE =
      """
      <!DOCTYPE html>
      <html lang="en">
      <head>
          <meta charset="UTF-8">
          <link rel="icon" type="image/svg+xml" href="%1$s/mcp.svg">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>MCP Inspector</title>
          <script type="module" src=".%2$s/static/initScript-B8iPFz0O.js"></script>
          <script type="module" crossorigin src="%1$s/assets/index-B8iPFz0O.js"></script>
          <link rel="stylesheet" crossorigin href="%1$s/assets/index-DdtP67NK.css">
      </head>
      <body>
          <div id="root" class="w-full"></div>
          <input type="hidden" id="contextPath" value="%2$s" />
      </body>
      %3$s
      </html>
      """;

  private static final String AUTO_CONNECT_SCRIPT =
      """
      <script src=".%s/static/autoConnectScript-B8iPFz0O.js"></script>\
      """;

  private static final String DEFAULT_ENDPOINT = "/mcp-inspector";
  public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

  private String inspectorEndpoint = DEFAULT_ENDPOINT;
  private boolean autoConnect = true;
  private String defaultServer;
  private McpServerConfig mcpSrvConfig;
  private String indexHtml;

  public McpInspectorModule path(String inspectorEndpoint) {
    this.inspectorEndpoint = inspectorEndpoint;
    return this;
  }

  public McpInspectorModule autoConnect(boolean autoConnect) {
    this.autoConnect = autoConnect;
    return this;
  }

  public McpInspectorModule defaultServer(String mcpServerName) {
    this.defaultServer = mcpServerName;
    return this;
  }

  @Override
  public void install(Jooby app) {
    this.indexHtml = buildIndexHtml();

    app.assets(inspectorEndpoint + "/static/*", "/mcpInspector/assets/");

    app.get(inspectorEndpoint, ctx -> ctx.setResponseType(MediaType.html).render(this.indexHtml));

    app.get(
        inspectorEndpoint + "/config",
        ctx -> {
          var location = resolveLocation(ctx);
          var configJson = buildConfigJson(mcpSrvConfig, location);
          return ctx.setResponseType(MediaType.json).render(configJson);
        });

    app.onStarting(
        () -> {
          this.mcpSrvConfig = resolveMcpServerConfig(app);
        });
  }

  private String buildIndexHtml() {
    var script = this.autoConnect ? AUTO_CONNECT_SCRIPT.formatted(inspectorEndpoint) : "";
    return INDEX_HTML_TEMPLATE.formatted(DIST, inspectorEndpoint, script);
  }

  private String resolveLocation(Context ctx) {
    var scheme = resolveSchema(ctx);
    if (ctx.getPort() == 80) {
      return scheme + "://" + ctx.getHost();
    } else {
      return scheme + "://" + ctx.getHostAndPort();
    }
  }

  private String resolveSchema(Context ctx) {
    if (ctx.header(X_FORWARDED_PROTO).isPresent()) {
      return ctx.header(X_FORWARDED_PROTO).value();
    } else {
      return ctx.getScheme();
    }
  }

  private McpServerConfig resolveMcpServerConfig(Jooby app) {
    List<McpServerConfig> srvConfigs;
    try {
      srvConfigs = app.getServices().get(Reified.list(McpServerConfig.class));
    } catch (RegistryException ex) {
      throw new StartupException(
          "MCP Inspector module requires at least one MCP server to be configured.");
    }

    if (defaultServer != null) {
      return srvConfigs.stream()
          .filter(config -> config.getName().equals(defaultServer))
          .findFirst()
          .orElseThrow(
              () ->
                  new StartupException("MCP server named '%s' not found".formatted(defaultServer)));
    }

    return srvConfigs.getFirst();
  }

  private String buildConfigJson(McpServerConfig config, String location) {
    var endpoint = resolveEndpoint(config);
    var transport =
        config.isSseTransport() ? McpModule.Transport.SSE : McpModule.Transport.STREAMABLE_HTTP;
    return """
    {
      "defaultEnvironment": {
      },
      "defaultCommand": "",
      "defaultArgs": "",
      "defaultTransport": "%s",
      "defaultServerUrl": "%s%s"
    }
    """
        .formatted(transport.getValue(), location, endpoint);
  }

  private String resolveEndpoint(McpServerConfig config) {
    if (config.isSseTransport()) {
      return config.getSseEndpoint();
    } else {
      return config.getMcpEndpoint();
    }
  }
}
