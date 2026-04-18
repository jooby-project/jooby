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
 *   <li><b>Transport Flexibility:</b> Supports {@link
 *       io.jooby.mcp.McpModule.Transport#STREAMABLE_HTTP} (default), {@link
 *       io.jooby.mcp.McpModule.Transport#SSE}, {@link io.jooby.mcp.McpModule.Transport#WEBSOCKET},
 *       and {@link io.jooby.mcp.McpModule.Transport#STATELESS_STREAMABLE_HTTP}.
 *   <li><b>Execution Interception:</b> Chain custom {@link io.jooby.mcp.McpInvoker} instances to
 *       seamlessly inject MDC context, telemetry, or custom error handling around executions.
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
 * tool, prompt, or resource call by providing a custom {@link io.jooby.mcp.McpInvoker}:
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
 * <p>The generated {@link io.jooby.mcp.McpService} instances do not represent servers themselves;
 * they are mapped to specific server instances using the {@code @McpServer("serverKey")} annotation
 * on your original class.
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
 * install the {@link io.jooby.mcp.McpInspectorModule} alongside this module to interactively
 * execute your tools, prompts, and resources straight from your browser.
 *
 * @author kliushnichenko
 * @author edgar
 * @since 4.2.0
 */
@org.jspecify.annotations.NullMarked
package io.jooby.mcp;
