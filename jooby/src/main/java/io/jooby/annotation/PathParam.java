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
 * Allow access to path variable from MVC route method.
 *
 * <pre>{@code
 * &#64;Path("/:id")
 * public String findById(&#64;PathParam String id) {
 *   ...
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PathParam {
  /**
   * Parameter name. Defaults to method parameter name.
   *
   * @return Parameter name. Defaults to method parameter name.
   */
  String name() default "";
}
