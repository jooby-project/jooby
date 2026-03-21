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

/** Marks a class as an MCP (Model Context Protocol) Server. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpServer {
  /**
   * The server key used to look up configuration in application.conf. Defaults to "default".
   *
   * @return The server configuration key.
   */
  String value() default "default";
}
