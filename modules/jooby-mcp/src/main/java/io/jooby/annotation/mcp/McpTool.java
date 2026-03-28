/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation.mcp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Exposes a method as an MCP Tool. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {
  /**
   * The name of the tool. If empty, the method name is used.
   *
   * @return Tool name.
   */
  String name() default "";

  /**
   * A description of what the tool does. Highly recommended for LLM usage.
   *
   * @return Tool description.
   */
  String description() default "";
}
