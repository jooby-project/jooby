/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import java.util.Map;

import io.modelcontextprotocol.server.McpSyncServerExchange;

/**
 * @author kliushnichenko
 */
@FunctionalInterface
public interface MethodInvoker {
  /**
   * Invokes a method with the provided arguments and exchange context.
   *
   * @param args a map of argument names to values
   * @param exchange the server exchange context
   * @return the result of the method invocation
   */
  Object invoke(final Map<String, Object> args, McpSyncServerExchange exchange);
}
