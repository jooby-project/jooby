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
 * Defines the HTMX error template to render if a validation or parameter binding exception occurs.
 *
 * @author edgar
 * @since 4.5.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface HxError {
  /**
   * The fallback template to render if a validation or parameter binding exception occurs.
   *
   * @return The error template path.
   */
  String value();

  /**
   * Automatically appends an {@code HX-Retarget} header when an exception triggers the {@link
   * #value()} ()}. Useful for redirecting failed form submissions back to the form container
   * instead of the default target.
   *
   * @return The CSS selector of the error target.
   */
  String target() default "";
}
