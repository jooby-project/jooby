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
 * Instructs HTMX to override the client-side swap style for the response. Maps to the {@code
 * HX-Reswap} header.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface HxSwap {
  /**
   * The HTMX swap style, optionally including modifiers.
   *
   * <p>Examples: {@code "innerHTML"}, {@code "outerHTML"}, {@code "outerHTML scroll:top"}
   *
   * @return The swap style string.
   */
  String value();
}
