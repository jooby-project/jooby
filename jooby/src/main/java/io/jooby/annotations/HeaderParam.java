/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow access to header value from MVC route method.
 *
 * <pre>{@code
 *  public String header(&#64;HeaderParam String version) {
 *    ...
 *  }
 *
 *  public String form(&#64;HeaderParam("If-Modified-Since") long ifModifiedSince) {
 *    ...
 *  }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface HeaderParam {

  /**
   * Parameter name.
   *
   * @return Parameter name.
   */
  String value() default "";
}
