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
 * Custom mapping of HTTP request to parameter.
 *
 * @author edgar
 * @since 3.2.5
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface BindParam {
  /**
   * Class containing the mapping function.
   *
   * @return Default to controller class.
   */
  Class<?> value() default void.class;

  /**
   * Name of the function doing the mapping. Function must accept a single argument of type {@link
   * io.jooby.Context} and returns type must be the parameter type. Name is optional and only
   * required in case of conflict.
   *
   * @return Name of the function doing the mapping.
   */
  String fn() default "";
}
