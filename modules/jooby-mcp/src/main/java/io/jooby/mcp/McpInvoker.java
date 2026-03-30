/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import io.jooby.SneakyThrows;

/**
 * Intercepts and wraps the execution of MCP (Model Context Protocol) operations, such as tools,
 * prompts, resources, and completions.
 *
 * <p>The {@link McpInvoker} acts as a middleware or decorator around the generated MCP routing
 * logic. It allows you to seamlessly inject cross-cutting concerns—such as telemetry, logging
 * (SLF4J MDC), transaction management, or custom error handling—right before and after an operation
 * executes.
 *
 * <h2>Chaining Invokers</h2>
 *
 * <p>Jooby provides a default internal invoker that gracefully maps standard framework exceptions
 * to MCP JSON-RPC errors. When you register a custom invoker via {@link
 * io.jooby.mcp.McpModule#invoker(McpInvoker)}, the framework automatically chains your custom
 * invoker with the default one using the {@link #then(McpInvoker)} method.
 *
 * <h3>Example: MDC Context Propagation</h3>
 *
 * <pre>{@code
 * public class MdcMcpInvoker implements McpInvoker {
 * public <R> R invoke(String operationId, SneakyThrows.Supplier<R> action) {
 * try {
 * MDC.put("mcp.operation", operationId);
 * // Execute the actual tool or proceed to the next invoker in the chain
 * return action.get();
 * } finally {
 * MDC.remove("mcp.operation");
 * }
 * }
 * }
 * * // Register and automatically chain it:
 * install(new McpModule(new MyServiceMcp_())
 * .invoker(new MdcMcpInvoker()));
 * }</pre>
 *
 * @author edgar
 * @since 4.2.0
 */
public interface McpInvoker {

  /**
   * Executes the given MCP operation.
   *
   * @param operation The operation being executed.
   * @param action The actual execution of the operation, or the next invoker in the chain. Must be
   *     invoked via {@link SneakyThrows.Supplier#get()} to proceed.
   * @param <R> The return type of the operation.
   * @return The result of the operation.
   */
  <R> R invoke(McpOperation operation, SneakyThrows.Supplier<R> action);

  /**
   * Chains this invoker with another one. This invoker runs first, and its "action" becomes calling
   * the next invoker.
   *
   * <p>This is used internally by {@link io.jooby.mcp.McpModule} to compose user-provided invokers
   * with the default framework exception mappers.
   *
   * @param next The next invoker in the chain.
   * @return A composed invoker.
   */
  default McpInvoker then(McpInvoker next) {
    if (next == null) {
      return this;
    }
    return new McpInvoker() {
      @Override
      public <R> R invoke(McpOperation operation, SneakyThrows.Supplier<R> action) {
        return McpInvoker.this.invoke(operation, () -> next.invoke(operation, action));
      }
    };
  }
}
