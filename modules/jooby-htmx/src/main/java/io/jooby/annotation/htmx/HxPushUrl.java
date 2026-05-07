/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation.htmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instructs HTMX to push a new URL into the browser's history stack. Maps to the {@code
 * HX-Push-Url} header.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface HxPushUrl {
  /**
   * The URL to push to the history stack.
   *
   * <p>Use {@code "true"} (the default) to push the current request URL. Use {@code "false"} to
   * explicitly prevent history pushing. Provide a path (e.g., {@code "/users/list"}) to push a
   * specific URL.
   *
   * @return The URL directive.
   */
  String value() default "true";
}
