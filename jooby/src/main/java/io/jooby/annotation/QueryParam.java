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
 * Allow access to query parameter from MVC route method.
 *
 * <pre>{@code
 * public String search(&#64;QueryParam String q) {
 *   ...
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface QueryParam {
  /**
   * Parameter name. Defaults to method parameter name.
   *
   * @return Parameter name. Defaults to method parameter name.
   */
  String name() default "";
}
