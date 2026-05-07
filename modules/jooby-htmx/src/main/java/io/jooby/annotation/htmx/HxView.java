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

  /**
   * Defines the outer HTML layout (or "SPA Shell") that wraps this partial view.
   *
   * <p>This attribute enables seamless deep-linking and full-page refreshes in an HTMX application.
   * It allows a single controller method to serve both dynamic UI fragments and fully-formed HTML
   * pages depending on the origin of the incoming request.
   *
   * <h3>How it works:</h3>
   *
   * <ul>
   *   <li><b>HTMX Requests:</b> If the request contains the {@code HX-Request: true} header, this
   *       layout attribute is completely ignored. The framework responds only with the partial view
   *       defined in the primary {@code value()} attribute, ensuring fast, targeted DOM swaps.
   *   <li><b>Standard Browser Requests:</b> If a user accesses the endpoint directly via the URL
   *       bar, a bookmark, or an {@code F5} refresh, the framework intercepts the request. It
   *       renders this layout file.
   * </ul>
   *
   * <h3>Template Integration:</h3>
   *
   * <p>When a layout fallback is triggered, the framework automatically injects the name of the
   * target partial view into the response model under the {@code childView} key. Your layout file
   * must use your template engine's dynamic include syntax to render the child view.
   *
   * @return The path to the layout template file, or an empty string if no layout is required.
   */
  String layout() default "";
}
