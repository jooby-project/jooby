/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines what media types a route can produces. By default a route can produces any type {@code
 * *}/{@code *}. Check the <code>Accept</code> header against this value or send a "406 Not
 * Acceptable" response.
 *
 * <pre>
 *   class Resources {
 *
 *     &#64;Produces("application/json")
 *     public Object method() {
 *      return ...;
 *     }
 *   }
 * </pre>
 *
 * @author edgar
 * @since 2.0.0
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Produces {
  /**
   * List of media types.
   *
   * @return Media types.
   */
  String[] value();
}
