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
 * Allow access to field or entire form from MVC route method.
 *
 * <pre>{@code
 *  public String formField(&#64;FormParm String name) {
 *    ...
 *  }
 *
 *  public String form(&#64;FormParam MyForm form) {
 *    ...
 *  }
 * }</pre>
 *
 * HTTP request must be encoded as {@link io.jooby.MediaType#FORM_URLENCODED} or
 * {@link io.jooby.MediaType#MULTIPART_FORMDATA}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FormParam {

  /**
   * Parameter name.
   *
   * @return Parameter name.
   */
  String value() default "";
}
