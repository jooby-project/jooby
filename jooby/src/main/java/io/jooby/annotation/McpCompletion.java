/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a method as an MCP Completion provider. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpCompletion {
  /**
   * The identifier of the reference. This is either the Prompt name (e.g., "code_review") or the
   * Resource Template URI (e.g., "file:///project/{name}").
   */
  String ref();

  /** The name of the argument or template variable being completed. */
  String arg();
}
