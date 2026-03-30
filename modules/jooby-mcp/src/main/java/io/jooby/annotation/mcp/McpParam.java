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

/** Provides metadata for an MCP Tool or Prompt parameter. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpParam {
  /**
   * The name of the parameter in the MCP schema. If empty, the Java variable name is used.
   *
   * @return Parameter name.
   */
  String name() default "";

  /**
   * A description of the parameter for the LLM. If empty, it falls back to the @param tag in the
   * method's Javadoc.
   *
   * @return Parameter description.
   */
  String description() default "";

  /**
   * Whether this parameter is required.
   *
   * @return True if required, false otherwise.
   */
  boolean required() default true;
}
