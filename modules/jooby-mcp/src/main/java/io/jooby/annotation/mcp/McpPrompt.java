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

/** Exposes a method as an MCP Prompt. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpPrompt {
  /**
   * The name of the prompt. If empty, the method name is used.
   *
   * @return Prompt name.
   */
  String name() default "";

  /** Optional human-readable name of the prompt for display purposes. */
  String title() default "";

  /**
   * A description of what the prompt provides.
   *
   * @return Prompt description.
   */
  String description() default "";
}
