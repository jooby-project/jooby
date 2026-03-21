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
  String value();

  /**
   * The name of the resource.
   *
   * @return Resource name.
   */
  String name() default "";

  /**
   * A description of the resource.
   *
   * @return Resource description.
   */
  String description() default "";

  /**
   * The MIME type of the resource (e.g., "text/plain", "application/json"). * @return The MIME
   * type.
   */
  String mimeType() default "";
}
