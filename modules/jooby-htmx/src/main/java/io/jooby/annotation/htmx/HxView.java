/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation.htmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the HTMX view rendering strategy for an MVC route.
 *
 * <p>This annotation is intercepted by the HTMX APT generator to produce a {@code ModelAndView}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface HxView {

  /**
   * The classpath location of the template file (e.g., "users/profile").
   *
   * @return The template path.
   */
  String value();
}
