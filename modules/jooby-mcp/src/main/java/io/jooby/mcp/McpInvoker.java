/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import io.jooby.SneakyThrows;

public interface McpInvoker {

  <R> R invoke(String operationId, SneakyThrows.Supplier<R> action);

  /**
   * Chains this invoker with another one. This invoker runs first, and its "action" becomes calling
   * the next invoker.
   *
   * @param next The next invoker in the chain.
   * @return A composed invoker.
   */
  default McpInvoker then(McpInvoker next) {
    return new McpInvoker() {
      @Override
      public <R> R invoke(String operationId, SneakyThrows.Supplier<R> action) {
        return McpInvoker.this.invoke(operationId, () -> next.invoke(operationId, action));
      }
    };
  }
}
