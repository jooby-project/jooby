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
 * Instructs HTMX to swap the response into a different target element than the one that initiated
 * the request. Maps to the {@code HX-Retarget} header.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface HxTarget {
  /**
   * The CSS selector of the target element.
   *
   * @return The CSS selector.
   */
  String value();
}
