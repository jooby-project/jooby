/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotations;

import io.jooby.Context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow access to context attributes from MVC route.
 *
 * <pre>{@code
 *  public String method(&#64;ContextParam String version) {
 *    ...
 *  }
 * }</pre>
 *
 * See {@link Context#getAttributes()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ContextParam {

  /**
   * Attribute's name. See {@link io.jooby.Context#attribute(String)}
   *
   * @return Attribute's name.
   */
  String value() default "";
}
