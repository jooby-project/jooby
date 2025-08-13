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

import io.jooby.ParamSource;

/**
 * Allow access to a parameter from MVC route method and from multiple sources.
 *
 * <p>The order of {@link ParamSource}s defines the search priority.
 *
 * <pre>{@code
 * public String search(&#64;Param({ ParamSource.QUERY, ParamSource.PATH }) String q) {
 *   ...
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {

  /**
   * Parameter name. Defaults to method parameter name.
   *
   * @return Parameter name. Defaults to method parameter name.
   */
  String name() default "";

  /**
   * Parameter sources to search in, the order defines the search priority.
   *
   * @return Parameter sources to search in.
   */
  ParamSource[] value();
}
