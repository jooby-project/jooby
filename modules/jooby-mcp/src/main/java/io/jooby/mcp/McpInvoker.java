/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import java.util.Objects;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import io.jooby.SneakyThrows;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Intercepts and wraps the execution of MCP (Model Context Protocol) operations, such as tools,
 * prompts, resources, and completions.
 *
 * <p>The {@link McpInvoker} acts as a middleware or decorator around the generated MCP routing
 * logic. It allows you to seamlessly inject cross-cutting concerns—such as telemetry, logging
 * (SLF4J MDC), transaction management, or custom error handling—right before and after an operation
 * executes.
 *
 * <p>Additionally, it serves as a factory for adapting framework-agnostic handler functions into
 * the specific functional interfaces required by the underlying MCP Java SDK for both stateful and
 * stateless servers.
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
 * @Override
 * public <R> R invoke(@Nullable McpSyncServerExchange exchange, McpTransportContext transportContext, McpOperation operation, McpChain chain) throws Exception {
 * try {
 * MDC.put("mcp.operation", operation.id());
 * // Execute the actual tool or proceed to the next invoker in the chain
 * return (R) chain.proceed(exchange, transportContext, operation);
 * } finally {
 * MDC.remove("mcp.operation");
 * }
 * }
 * }
 * * // Register and automatically chain it:
 * install(new McpModule(new MyServiceMcp_()).invoker(new MdcMcpInvoker()));
 * }</pre>
 *
 * @author edgar
 * @since 4.2.0
 */
public interface McpInvoker {

  /**
   * Executes the given MCP operation, allowing for pre- and post-processing.
   *
   * @param exchange The stateful server exchange, or {@code null} if running in a stateless
   *     context.
   * @param transportContext The transport context for the current connection.
   * @param operation The operation context containing the routing metadata and arguments.
   * @param next The chain used to proceed to the next invoker or the final handler.
   * @param <R> The expected return type of the MCP operation result.
   * @return The result of the operation.
   * @throws Exception If an error occurs during execution.
   */
  <R> R invoke(
      @Nullable McpSyncServerExchange exchange,
      McpTransportContext transportContext,
      McpOperation operation,
      McpChain next)
      throws Exception;

  /**
   * Adapts a framework function into a stateful Tool handler for the MCP SDK.
   *
   * @param operationId The standard MCP routing identifier (e.g., "tools/add_numbers").
   * @param classname The fully qualified name of the target class.
   * @param method The name of the target method.
   * @param fn The framework function to execute.
   * @return A {@link BiFunction} compatible with {@code McpSyncServer}.
   */
  default BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult>
      asToolHandler(
          String operationId,
          String classname,
          String method,
          SneakyThrows.Function3<
                  McpSyncServerExchange,
                  McpTransportContext,
                  McpOperation,
                  McpSchema.CallToolResult>
              fn) {
    return (exchange, req) -> {
      var operation = McpOperation.create(operationId, classname, method, req);
      try {
        return McpInvoker.this.invoke(
            exchange,
            exchange.transportContext(),
            operation,
            new McpChain() {
              @SuppressWarnings("unchecked")
              @Override
              public McpSchema.CallToolResult proceed(
                  @Nullable McpSyncServerExchange exchange,
                  McpTransportContext transportContext,
                  McpOperation operation) {
                return fn.apply(exchange, transportContext, operation);
              }
            });
      } catch (Exception e) {
        throw SneakyThrows.propagate(e);
      }
    };
  }

  /**
   * Adapts a framework function into a stateless Tool handler for the MCP SDK.
   *
   * @param operationId The standard MCP routing identifier (e.g., "tools/add_numbers").
   * @param classname The fully qualified name of the target class.
   * @param method The name of the target method.
   * @param fn The framework function to execute.
   * @return A {@link BiFunction} compatible with {@code McpStatelessSyncServer}.
   */
  default BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult>
      asStatelessToolHandler(
          String operationId,
          String classname,
          String method,
          SneakyThrows.Function3<
                  McpSyncServerExchange,
                  McpTransportContext,
                  McpOperation,
                  McpSchema.CallToolResult>
              fn) {
    return (transportContext, req) -> {
      var operation = McpOperation.create(operationId, classname, method, req);
      try {
        return McpInvoker.this.invoke(
            null,
            transportContext,
            operation,
            new McpChain() {
              @SuppressWarnings("unchecked")
              @Override
              public McpSchema.CallToolResult proceed(
                  @Nullable McpSyncServerExchange exchange,
                  McpTransportContext transportContext,
                  McpOperation operation) {
                return fn.apply(exchange, transportContext, operation);
              }
            });
      } catch (Exception e) {
        throw SneakyThrows.propagate(e);
      }
    };
  }

  /**
   * Adapts a framework function into a stateful Prompt handler for the MCP SDK.
   *
   * @param operationId The standard MCP routing identifier (e.g., "tools/add_numbers").
   * @param classname The fully qualified name of the target class.
   * @param method The name of the target method.
   * @param fn The framework function to execute.
   * @return A {@link BiFunction} compatible with {@code McpSyncServer}.
   */
  default BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult>
      asPromptHandler(
          String operationId,
          String classname,
          String method,
          SneakyThrows.Function3<
                  McpSyncServerExchange,
                  McpTransportContext,
                  McpOperation,
                  McpSchema.GetPromptResult>
              fn) {
    return (exchange, req) -> {
      var operation = McpOperation.create(operationId, classname, method, req);
      try {
        return McpInvoker.this.invoke(
            exchange,
            exchange.transportContext(),
            operation,
            new McpChain() {
              @SuppressWarnings("unchecked")
              @Override
              public McpSchema.GetPromptResult proceed(
                  @Nullable McpSyncServerExchange exchange,
                  McpTransportContext transportContext,
                  McpOperation operation) {
                return fn.apply(exchange, transportContext, operation);
              }
            });
      } catch (Exception e) {
        throw SneakyThrows.propagate(e);
      }
    };
  }

  /**
   * Adapts a framework function into a stateless Prompt handler for the MCP SDK.
   *
   * @param operationId The standard MCP routing identifier (e.g., "tools/add_numbers").
   * @param classname The fully qualified name of the target class.
   * @param method The name of the target method.
   * @param fn The framework function to execute.
   * @return A {@link BiFunction} compatible with {@code McpStatelessSyncServer}.
   */
  default BiFunction<McpTransportContext, McpSchema.GetPromptRequest, McpSchema.GetPromptResult>
      asStatelessPromptHandler(
          String operationId,
          String classname,
          String method,
          SneakyThrows.Function3<
                  McpSyncServerExchange,
                  McpTransportContext,
                  McpOperation,
                  McpSchema.GetPromptResult>
              fn) {
    return (transportContext, req) -> {
      var operation = McpOperation.create(operationId, classname, method, req);
      try {
        return McpInvoker.this.invoke(
            null,
            transportContext,
            operation,
            new McpChain() {
              @SuppressWarnings("unchecked")
              @Override
              public McpSchema.GetPromptResult proceed(
                  @Nullable McpSyncServerExchange exchange,
                  McpTransportContext transportContext,
                  McpOperation operation) {
                return fn.apply(exchange, transportContext, operation);
              }
            });
      } catch (Exception e) {
        throw SneakyThrows.propagate(e);
      }
    };
  }

  /**
   * Adapts a framework function into a stateful Resource handler for the MCP SDK.
   *
   * @param operationId The standard MCP routing identifier (e.g., "tools/add_numbers").
   * @param classname The fully qualified name of the target class.
   * @param method The name of the target method.
   * @param fn The framework function to execute.
   * @return A {@link BiFunction} compatible with {@code McpSyncServer}.
   */
  default BiFunction<
          McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult>
      asResourceHandler(
          String operationId,
          String classname,
          String method,
          SneakyThrows.Function3<
                  McpSyncServerExchange,
                  McpTransportContext,
                  McpOperation,
                  McpSchema.ReadResourceResult>
              fn) {
    return (exchange, req) -> {
      var operation = McpOperation.create(operationId, classname, method, req);
      try {
        return McpInvoker.this.invoke(
            exchange,
            exchange.transportContext(),
            operation,
            new McpChain() {
              @SuppressWarnings("unchecked")
              @Override
              public McpSchema.ReadResourceResult proceed(
                  @Nullable McpSyncServerExchange exchange,
                  McpTransportContext transportContext,
                  McpOperation operation) {
                return fn.apply(exchange, transportContext, operation);
              }
            });
      } catch (Exception e) {
        throw SneakyThrows.propagate(e);
      }
    };
  }

  /**
   * Adapts a framework function into a stateless Resource handler for the MCP SDK.
   *
   * @param operationId The standard MCP routing identifier (e.g., "tools/add_numbers").
   * @param classname The fully qualified name of the target class.
   * @param method The name of the target method.
   * @param fn The framework function to execute.
   * @return A {@link BiFunction} compatible with {@code McpStatelessSyncServer}.
   */
  default BiFunction<
          McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult>
      asStatelessResourceHandler(
          String operationId,
          String classname,
          String method,
          SneakyThrows.Function3<
                  McpSyncServerExchange,
                  McpTransportContext,
                  McpOperation,
                  McpSchema.ReadResourceResult>
              fn) {
    return (transportContext, req) -> {
      var operation = McpOperation.create(operationId, classname, method, req);
      try {
        return McpInvoker.this.invoke(
            null,
            transportContext,
            operation,
            new McpChain() {
              @SuppressWarnings("unchecked")
              @Override
              public McpSchema.ReadResourceResult proceed(
                  @Nullable McpSyncServerExchange exchange,
                  McpTransportContext transportContext,
                  McpOperation operation) {
                return fn.apply(exchange, transportContext, operation);
              }
            });
      } catch (Exception e) {
        throw SneakyThrows.propagate(e);
      }
    };
  }

  /**
   * Adapts a framework function into a stateful Completion handler for the MCP SDK.
   *
   * @param operationId The standard MCP routing identifier (e.g., "tools/add_numbers").
   * @param classname The fully qualified name of the target class.
   * @param method The name of the target method.
   * @param fn The framework function to execute.
   * @return A {@link BiFunction} compatible with {@code McpSyncServer}.
   */
  default BiFunction<McpSyncServerExchange, McpSchema.CompleteRequest, McpSchema.CompleteResult>
      asCompletionHandler(
          String operationId,
          String classname,
          String method,
          SneakyThrows.Function3<
                  McpSyncServerExchange,
                  McpTransportContext,
                  McpOperation,
                  McpSchema.CompleteResult>
              fn) {
    return (exchange, req) -> {
      var operation = McpOperation.create(operationId, classname, method, req);
      try {
        return McpInvoker.this.invoke(
            exchange,
            exchange.transportContext(),
            operation,
            new McpChain() {
              @SuppressWarnings("unchecked")
              @Override
              public McpSchema.CompleteResult proceed(
                  @Nullable McpSyncServerExchange exchange,
                  McpTransportContext transportContext,
                  McpOperation operation) {
                return fn.apply(exchange, transportContext, operation);
              }
            });
      } catch (Exception e) {
        throw SneakyThrows.propagate(e);
      }
    };
  }

  /**
   * Adapts a framework function into a stateless Completion handler for the MCP SDK.
   *
   * @param operationId The standard MCP routing identifier (e.g., "tools/add_numbers").
   * @param classname The fully qualified name of the target class.
   * @param method The name of the target method.
   * @param fn The framework function to execute.
   * @return A {@link BiFunction} compatible with {@code McpStatelessSyncServer}.
   */
  default BiFunction<McpTransportContext, McpSchema.CompleteRequest, McpSchema.CompleteResult>
      asStatelessCompletionHandler(
          String operationId,
          String classname,
          String method,
          SneakyThrows.Function3<
                  McpSyncServerExchange,
                  McpTransportContext,
                  McpOperation,
                  McpSchema.CompleteResult>
              fn) {
    return (transportContext, req) -> {
      var operation = McpOperation.create(operationId, classname, method, req);
      try {
        return McpInvoker.this.invoke(
            null,
            transportContext,
            operation,
            new McpChain() {
              @SuppressWarnings("unchecked")
              @Override
              public McpSchema.CompleteResult proceed(
                  @Nullable McpSyncServerExchange exchange,
                  McpTransportContext transportContext,
                  McpOperation operation) {
                return fn.apply(exchange, transportContext, operation);
              }
            });
      } catch (Exception e) {
        throw SneakyThrows.propagate(e);
      }
    };
  }

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
    Objects.requireNonNull(next, "next invoker is required");
    return new McpInvoker() {
      @Override
      public <R> R invoke(
          @Nullable McpSyncServerExchange exchange,
          McpTransportContext transportContext,
          McpOperation operation,
          McpChain chain)
          throws Exception {
        return McpInvoker.this.invoke(
            exchange,
            transportContext,
            operation,
            new McpChain() {
              @Override
              public <T> T proceed(
                  @Nullable McpSyncServerExchange chainExchange,
                  McpTransportContext chainTransportContext,
                  McpOperation chainOperation)
                  throws Exception {
                return next.invoke(chainExchange, chainTransportContext, chainOperation, chain);
              }
            });
      }
    };
  }
}
