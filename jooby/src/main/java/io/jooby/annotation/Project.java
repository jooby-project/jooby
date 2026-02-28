/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation;

import java.lang.annotation.*;

/**
 * Declarative JSON projection for route handlers.
 *
 * <p>When applied to a method or class, Jooby automatically filters the JSON output to include only
 * the specified fields. *
 *
 * <h3>String Notation Support:</h3>
 *
 * <ul>
 *   <li><b>Dot Notation:</b> {@code "address.city"}
 *   <li><b>Avaje Notation:</b> {@code "address(city, zip)"}
 * </ul>
 *
 * *
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * @GET
 * @Project({"id", "name", "address(city, zip)"})
 * public User getUser() {
 * return userService.find(1);
 * }
 * }</pre>
 *
 * Or
 *
 * <pre>{@code
 * @GET(projection = "id, name, address(city, zip)")
 * public User getUser() {
 * return userService.find(1);
 * }
 * }</pre>
 *
 * @author edgar
 * @since 4.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Project {
  /**
   * Example: {@code "id, name, address(city, zip)"}
   *
   * @return Avaje notation.
   */
  String value() default "";
}
