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

/**
 * Controls the generation of the JSON output schema for this tool.
 *
 * <p>By default, output schema generation is controlled by a global flag on the MCP Module (which
 * defaults to false to save LLM context window tokens). Applying any of the annotations in this
 * group to a tool method will explicitly override the global flag.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpOutputSchema {

  /**
   * Forces the generation of an output schema based on the provided class, regardless of the global
   * module configuration.
   *
   * <p>This is especially useful when the method's actual return type is generic (e.g., {@code
   * Object} or {@code Response}) due to type erasure, but you want the LLM to know the exact JSON
   * shape.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface From {
    Class<?> value();
  }

  /**
   * Forces the generation of an array output schema based on the provided class, regardless of the
   * global module configuration.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface ArrayOf {
    Class<?> value();
  }

  /**
   * Forces the generation of a Map output schema (String keys to the provided class values),
   * regardless of the global module configuration.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface MapOf {
    Class<?> value();
  }

  /**
   * Explicitly disables output schema generation for this specific tool, even if the global module
   * flag is set to true.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Off {}
}
