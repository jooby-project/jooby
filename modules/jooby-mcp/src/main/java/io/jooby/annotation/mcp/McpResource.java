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
 * Exposes a method as an MCP Resource or Resource Template.
 *
 * <p>If the URI contains path variables (e.g., "file:///{dir}/{filename}"), it will be treated as a
 * ResourceTemplate.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpResource {
  /**
   * The exact URI or URI template for the resource.
   *
   * @return The resource URI.
   */
  String uri();

  /**
   * The name of the resource.
   *
   * @return Resource name.
   */
  String name() default "";

  /** Optional human-readable name of the prompt for display purposes. */
  String title() default "";

  /**
   * A description of the resource.
   *
   * @return Resource description.
   */
  String description() default "";

  /**
   * The MIME type of the resource (e.g., "text/plain", "application/json").
   *
   * @return The MIME type.
   */
  String mimeType() default "";

  /** Optional size in bytes. */
  int size() default -1;

  /** Optional MCP metadata annotations for this resource. */
  McpAnnotations[]
      annotations() default {}; // Using an array is the safest way to provide an "empty" default in

  // Java annotations

  enum Role {
    USER,
    ASSISTANT
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.ANNOTATION_TYPE)
  @interface McpAnnotations {

    /**
     * Describes who the intended customer of this object or data is. It can include multiple
     * entries to indicate content useful for multiple audiences (e.g., [“user”, “assistant”]).
     */
    Role[] audience();

    /** The date and time (in ISO 8601 format) when the resource was last modified. */
    String lastModified() default "";

    /**
     * Describes how important this data is for operating the server.
     *
     * <p>A value of 1 means “most important,” and indicates that the data is effectively required,
     * while 0 means “least important,” and indicates that the data is entirely optional.
     */
    double priority();
  }
}
