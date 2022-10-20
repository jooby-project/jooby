/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines what media types a route can consume. By default a route can consume any type {@code *}/
 * {@code *}.
 *
 * <p>Check the <code>Content-Type</code> header against this value or send a "415 Unsupported Media
 * Type" response.
 *
 * <pre>
 *   class Resources {
 *
 *     &#64;Consumes("application/json")
 *     public void method(&#64;Body MyBody body) {
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
public @interface Consumes {
  /**
   * List of media types.
   *
   * @return Media types.
   */
  String[] value();
}
