/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import org.jspecify.annotations.Nullable;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;

/**
 * Represents a chain of interceptors for an MCP operation.
 *
 * <p>When an MCP operation is executed, it passes through a chain of {@link McpInvoker} instances.
 * The {@code McpChain} is responsible for yielding control to the next invoker in the chain, or
 * finally executing the target handler if there are no more interceptors.
 *
 * @author edgar
 * @since 4.2.0
 */
public interface McpChain {

  /**
   * Proceeds to the next interceptor in the chain or executes the target handler.
   *
   * <p>Interceptors can modify the {@link McpOperation} (e.g., sanitizing arguments) before passing
   * it down the chain.
   *
   * @param exchange The stateful server exchange, or {@code null} if running in a stateless
   *     context.
   * @param transportContext The transport context for the current connection.
   * @param operation The operation context containing the routing ID and arguments.
   * @return The result of the operation execution.
   * @throws Exception If the downstream execution fails.
   */
  <R> R proceed(
      @Nullable McpSyncServerExchange exchange,
      McpTransportContext transportContext,
      McpOperation operation)
      throws Exception;
}
