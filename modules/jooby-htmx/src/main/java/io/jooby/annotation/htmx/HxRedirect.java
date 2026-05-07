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
 * Instructs HTMX to perform a full-page client-side redirect to a new URL, bypassing standard swap
 * logic. Maps to the {@code HX-Redirect} header.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface HxRedirect {
  /**
   * The URL to redirect the client to.
   *
   * @return The destination URL.
   */
  String value();
}
