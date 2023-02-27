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
 * Set a path for Mvc routes.
 *
 * <pre>
 *   &#64;Path("/r")
 *   class Resources {
 *
 *     &#64;GET("/sub")
 *     public void method() {
 *     }
 *   }
 * </pre>
 *
 * <h2>Path Patterns</h2>
 *
 * <p>Jooby supports Ant-style path patterns:
 *
 * <p>Some examples:
 *
 * <ul>
 *   <li>{@code com/t?st.html} - matches {@code com/test.html} but also {@code com/tast.jsp} or
 *       {@code com/txst.html}
 *   <li>{@code com/*.html} - matches all {@code .html} files in the {@code com} directory
 *   <li><code>com/{@literal **}/test.html</code> - matches all {@code test.html} files underneath
 *       the {@code com} path
 *   <li>{@code **}/{@code *} - matches any path at any level.
 *   <li>{@code *} - matches any path at any level, shorthand for {@code **}/{@code *}.
 * </ul>
 *
 * <h2>Variables</h2>
 *
 * <p>Jooby supports path parameters too:
 *
 * <p>Some examples:
 *
 * <ul>
 *   <li><code> /user/{id}</code> - /user/* and give you access to the <code>id</code> var.
 *   <li><code> /user/{id:\\d+}</code> - /user/[digits] and give you access to the numeric <code>id
 *       </code> var.
 * </ul>
 *
 * @author edgar
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Path {
  /**
   * @return Route path pattern.
   */
  String[] value();
}
