/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import io.jooby.SneakyThrows;

public class McpHandler {

  public static <R> R invoke(SneakyThrows.Supplier<R> action) {
    return action.get();
  }
}
