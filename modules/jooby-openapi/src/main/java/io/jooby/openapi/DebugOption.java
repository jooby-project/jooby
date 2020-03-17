/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

/**
 * Debug options for {@link OpenAPIGenerator#generate(String)} when enabled it prints byte code
 * at different levels.
 */
public enum DebugOption {
  /**
   * Print entire classes.
   */
  ALL,

  /**
   * Print the jump/link classes that where read to reach a route handler.
   */
  HANDLER_LINK,

  /**
   * Print a route handler byte code. This is the lambda or method reference that contains the
   * real code we need.
   */
  HANDLER
}
