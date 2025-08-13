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

import io.jooby.Session;

/**
 * Allow access to session attributes from MVC route.
 *
 * <pre>{@code
 * public String method(&#64;SessionParam String userId) {
 *   ...
 * }
 * }</pre>
 *
 * See {@link Session#toMap()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SessionParam {

  /**
   * Session attribute's name. Defaults to method parameter name.
   *
   * @return Session attribute's name. Defaults to method parameter name.
   */
  String name() default "";
}
