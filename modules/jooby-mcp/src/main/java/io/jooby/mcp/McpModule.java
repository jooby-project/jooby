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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.exception.StartupException;
import io.jooby.internal.mcp.DefaultMcpInvoker;
import io.jooby.internal.mcp.McpServerConfig;
import io.jooby.internal.mcp.transport.SseTransportProvider;
import io.jooby.internal.mcp.transport.StatelessTransportProvider;
import io.jooby.internal.mcp.transport.StreamableTransportProvider;
import io.jooby.internal.mcp.transport.WebSocketTransportProvider;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.*;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP (Model Context Protocol) module for Jooby.
 *
 * <p>The MCP module provides seamless integration with the Model Context Protocol, allowing your
 * application to act as a standardized AI context server. It automatically bridges your Java/Kotlin
 * methods with LLM clients by exposing them as Tools, Resources, and Prompts.
 *
 * <h2>Key Features</h2>
 *
 * <ul>
 *   <li><b>Compile-Time Discovery:</b> Automatically generates routing logic for {@code @McpTool},
 *       {@code @McpPrompt}, and {@code @McpResource} annotations with zero reflection overhead via
 *       APT.
 *   <li><b>Rich Schema Generation:</b> Tool and parameter descriptions are extracted directly from
 *       your MCP annotations, gracefully falling back to standard JavaDoc comments if omitted.
 *   <li><b>Transport Flexibility:</b> Supports {@link Transport#STREAMABLE_HTTP} (default), {@link
 *       Transport#SSE}, {@link Transport#WEBSOCKET}, and {@link
 *       Transport#STATELESS_STREAMABLE_HTTP}.
 *   <li><b>Execution Interception:</b> Chain custom {@link McpInvoker} instances to seamlessly
 *       inject MDC context, telemetry, or custom error handling around executions.
 *   <li><b>LLM Self-Healing:</b> Automatically catches internal business exceptions and translates
 *       them into valid MCP error payloads, allowing LLMs to auto-correct their own mistakes.
 * </ul>
 *
 * <h2>Basic Usage</h2>
 *
 * <p>By default, the module requires zero configuration in {@code application.conf} and will spin
 * up a single {@code streamable-http} server.
 *
 * <p>The module relies on Jackson for JSON-RPC message serialization. Here is the standard setup
 * using Jackson 3:
 *
 * <pre>{@code
 * {
 *  // 1. Install Jackson 3 support
 *  install(new Jackson3Module());
 *  install(new McpJackson3Module());
 *  // 2. Install the MCP module with your APT-generated McpService
 *  install(new McpModule(new MyServiceMcp_()));
 * }
 * }</pre>
 *
 * <i>Note: If your project still uses Jackson 2, simply swap the modules to {@code install(new
 * JacksonModule());} and {@code install(new McpJackson2Module());}.</i>
 *
 * <h2>Changing the Default Transport</h2>
 *
 * <p>If you want to use a different transport protocol for the default server, you can configure it
 * directly in the Java DSL:
 *
 * <pre>{@code
 * {
 * install(new McpModule(new MyServiceMcp_())
 * .transport(Transport.SSE)); // Or Transport.WEBSOCKET, Transport.STATELESS_STREAMABLE_HTTP
 * }
 * }</pre>
 *
 * <h2>Custom Invokers & Telemetry</h2>
 *
 * <p>You can inject custom logic (like SLF4J MDC context propagation or Tracing spans) around every
 * tool, prompt, or resource call by providing a custom {@link McpInvoker}:
 *
 * <pre>{@code
 * {
 * install(new McpModule(new MyServiceMcp_())
 * .invoker(new MyCustomMdcInvoker())); // Chains automatically with the Default Exception Mapper
 * }
 * }</pre>
 *
 * <h2>Multiple Servers</h2>
 *
 * <p>The generated {@link McpService} instances do not represent servers themselves; they are
 * mapped to specific server instances using the {@code @McpServer("serverKey")} annotation on your
 * original class.
 *
 * <p>If you route services to multiple, isolated servers, you <b>must</b> define their specific
 * configurations in your {@code application.conf}:
 *
 * <pre>{@code
 * {
 * // Jooby will boot two separate MCP servers based on the @McpServer mapping of these services
 * install(new McpModule(new DefaultServiceMcp_(), new CalculatorServiceMcp_()));
 * }
 * }</pre>
 *
 * <p>{@code application.conf}:
 *
 * <pre>{@code
 * mcp.calculator {
 * name: "calculator-mcp-server"
 * version: "1.0.0"
 * transport: "web-socket"
 * mcpEndpoint: "/mcp/calculator/ws"
 * }
 * }</pre>
 *
 * <h2>Testing and Debugging</h2>
 *
 * <p>For local development, Jooby provides a built-in UI to test your AI capabilities. Simply
 * install the {@link McpInspectorModule} alongside this module to interactively execute your tools,
 * prompts, and resources straight from your browser.
 *
 * @author kliushnichenko
 * @author edgar
 * @since 4.2.0
 */
public class McpModule implements Extension {

  private static final McpTransportContextExtractor<Context> CTX_EXTRACTOR =
      ctx -> {
        var transportContext = Map.<String, Object>of("HEADERS", ctx.headerMap(), "CTX", ctx);
        return McpTransportContext.create(transportContext);
      };

  private static final String MODULE_CONFIG_PREFIX = "mcp";
  private static final Logger log = LoggerFactory.getLogger(McpModule.class);

  private Transport defaultTransport = STREAMABLE_HTTP;

  private final List<McpService> mcpServices = new ArrayList<>();

  private McpInvoker invoker;

  private Boolean generateOutputSchema = null;

  /**
   * Creates a new MCP module initialized with the provided generated services.
   *
   * <p>The services passed to this constructor are typically generated at compile-time by the Jooby
   * Annotation Processor (APT) for classes containing {@code @McpTool}, {@code @McpPrompt}, or
   * {@code @McpResource} annotations.
   *
   * @param mcpService The primary generated MCP service (usually suffixed with {@code Mcp_}).
   * @param mcpServices Optional additional generated MCP services to register.
   */
  public McpModule(McpService mcpService, McpService... mcpServices) {
    this.mcpServices.add(mcpService);
    if (mcpServices != null) {
      Collections.addAll(this.mcpServices, mcpServices);
    }
  }

  /**
   * Overrides the default transport protocol used by the MCP server.
   *
   * <p>If not explicitly called, the module defaults to {@link Transport#STREAMABLE_HTTP}. This
   * setting applies to the default server instance and can be overridden on a per-server basis via
   * your {@code application.conf}.
   *
   * @param transport The desired default transport protocol.
   * @return This module instance for method chaining.
   */
  public McpModule transport(@NonNull Transport transport) {
    this.defaultTransport = transport;
    return this;
  }

  /**
   * Registers a custom {@link McpInvoker} to intercept and wrap MCP operations.
   *
   * <p>This method allows you to inject cross-cutting concerns—such as logging, tracing, or SLF4J
   * MDC context propagation—around your tools, prompts, and resources.
   *
   * <p><b>Chaining:</b> If called multiple times, the newly provided invoker is chained
   * <i>before</i> the previously registered invokers. Ultimately, all custom invokers are
   * automatically chained just before the framework's default exception-mapping invoker.
   *
   * @param invoker The custom invoker to register.
   * @return This module instance for method chaining.
   */
  public McpModule invoker(@NonNull McpInvoker invoker) {
    if (this.invoker != null) {
      this.invoker = invoker.then(this.invoker);
    } else {
      this.invoker = invoker;
    }
    return this;
  }

  /**
   * Enabled/disables the generation of the output schema. It is automatically loaded from <code>
   * mcp.generateOutputSchema</code> which by default is <code>false</code>.
   *
   * @param generateOutputSchema <code>true</code> to enable the generation of the output schema.
   * @return This module instance for method chaining.
   */
  public McpModule generateOutputSchema(boolean generateOutputSchema) {
    this.generateOutputSchema = generateOutputSchema;
    return this;
  }

  @Override
  public void install(@NonNull Jooby app) {
    var services = app.getServices();
    var mcpJsonMapper = services.require(McpJsonMapper.class);
    var globalGenerateOutputSchema =
        app.getConfig().hasPath("mcp.generateOutputSchema")
            ? app.getConfig().getBoolean("mcp.generateOutputSchema")
            : Optional.ofNullable(this.generateOutputSchema).orElse(Boolean.FALSE);
    // invoker
    McpInvoker firstInvoker = new DefaultMcpInvoker(app);
    if (this.invoker != null) {
      firstInvoker = firstInvoker.then(this.invoker);
    }
    services.put(McpInvoker.class, firstInvoker);
    // Group services by server
    var mcpServiceMap = new HashMap<String, List<McpService>>();
    for (var mcpService : mcpServices) {
      var localGenerateOutputSchemaPath =
          MODULE_CONFIG_PREFIX + "." + mcpService.serverKey() + ".generateOutputSchema";
      var localGenerateOutputSchema =
          app.getConfig().hasPath(localGenerateOutputSchemaPath)
              ? app.getConfig().getBoolean(localGenerateOutputSchemaPath)
              : globalGenerateOutputSchema;
      mcpService.generateOutputSchema(localGenerateOutputSchema);
      mcpServiceMap.computeIfAbsent(mcpService.serverKey(), k -> new ArrayList<>()).add(mcpService);
    }
    // Boot everything
    for (var serverEntry : mcpServiceMap.entrySet()) {
      var mcpConfig = mcpServerConfig(app, serverEntry.getKey());
      // Internal usage only, required by mcp-inspector
      services.listOf(McpServerConfig.class).add(mcpConfig);

      var capabilitiesBuilder = new McpSchema.ServerCapabilities.Builder();
      serverEntry.getValue().forEach(it -> it.capabilities(capabilitiesBuilder));

      var capabilities = capabilitiesBuilder.build();
      boolean stateless;
      if (mcpConfig.getTransport() == STATELESS_STREAMABLE_HTTP) {
        var transport =
            new StatelessTransportProvider(app, mcpJsonMapper, mcpConfig, CTX_EXTRACTOR);
        var statelessServer =
            McpServer.sync(transport)
                .serverInfo(mcpConfig.getName(), mcpConfig.getVersion())
                .completions(statelessCompletions(app, serverEntry))
                .capabilities(capabilities)
                .instructions(mcpConfig.getInstructions())
                .build();
        // install services
        serverEntry
            .getValue()
            .forEach(throwingConsumer(service -> service.install(app, statelessServer)));
        // bind registry
        services.putIfAbsent(McpStatelessSyncServer.class, statelessServer);
        services.put(
            ServiceKey.key(McpStatelessSyncServer.class, serverEntry.getKey()), statelessServer);
        services.listOf(McpStatelessSyncServer.class).add(statelessServer);

        stateless = true;

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
                .completions(completions(app, serverEntry))
                .capabilities(capabilities)
                .instructions(mcpConfig.getInstructions())
                .build();
        // install service
        serverEntry
            .getValue()
            .forEach(throwingConsumer(service -> service.install(app, syncServer)));
        // bind registry
        services.putIfAbsent(McpSyncServer.class, syncServer);
        services.put(ServiceKey.key(McpSyncServer.class, serverEntry.getKey()), syncServer);
        services.listOf(McpSyncServer.class).add(syncServer);
        stateless = false;
        app.onStop(syncServer::close);
      }
      // Startup message:
      app.onStarting(
          () -> {
            var features = new ArrayList<String>();
            if (capabilities.tools() != null) features.add("Tools");
            if (capabilities.prompts() != null) features.add("Prompts");
            if (capabilities.resources() != null) features.add("Resources");
            var featuresStr = features.isEmpty() ? "None" : String.join(" | ", features);

            log.info(
                "MCP Server [{}({})] v{} online:",
                mcpConfig.getName(),
                serverEntry.getKey(),
                mcpConfig.getVersion());
            log.info("  ├─ Transport    : {}", mcpConfig.getTransport().getValue());
            if (!stateless) {
              log.info(
                  "  ├─ Keep-Alive   : {}",
                  mcpConfig.getKeepAliveInterval() == null
                      ? "N/A"
                      : mcpConfig.getKeepAliveInterval() + " s");
              log.info(
                  "  ├─ Session Rule : {}",
                  mcpConfig.isDisallowDelete() ? "Disallow Deletion (Strict)" : "Allow Deletion");
            }
            log.info("  ╰─ Capabilities : {}", featuresStr);
          });
    }
  }

  private static List<McpServerFeatures.SyncCompletionSpecification> completions(
      Jooby application, Map.Entry<String, List<McpService>> serverEntry) {
    return serverEntry.getValue().stream()
        .map(it -> it.completions(application))
        .flatMap(List::stream)
        .toList();
  }

  private static List<McpStatelessServerFeatures.SyncCompletionSpecification> statelessCompletions(
      Jooby application, Map.Entry<String, List<McpService>> serverEntry) {
    return serverEntry.getValue().stream()
        .map(it -> it.statelessCompletions(application))
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
    WEBSOCKET("web-socket");

    private final String value;

    Transport(String value) {
      this.value = value;
    }

    public static Transport of(String value) {
      for (var transport : values()) {
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
