/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Add common variables to as {@link Context} attributes so they are accessible from template
 * engine.
 *
 * Usage:
 *
 * <pre>{@code
 *    decorator(new WebVariables());
 *
 *   get("/", ctx -> new ModelAndView("index.ftl"));
 * }</pre>
 *
 * Template engine will be able to access to the following attributes:
 *
 * - contextPath. Empty when context path is set to <code>/</code> or actual context path.
 * - path. Current request path, as defined by {@link Context#pathString()}.
 * - user. Current user (if any) as defined by {@link Context#getUser()}.
 *
 * @author edgar
 * @since 2.4.0
 */
public class WebVariables implements Route.Decorator {

  private final String scope;

  /**
   * Creates a web variables under the given scope.
   *
   * @param scope Scope to use.
   */
  public WebVariables(@Nonnull String scope) {
    this.scope = scope;
  }

  /**
   * Creates a new web variables.
   */
  public WebVariables() {
    this.scope = null;
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> next.apply(webvariables(ctx));
  }

  private Context webvariables(Context ctx) {
    String contextPath = ctx.getContextPath();
    ctx.attribute(key("contextPath"), contextPath.equals("/") ? "" : contextPath);
    ctx.attribute(key("path"), ctx.pathString());
    Object user = ctx.getUser();
    if (user != null) {
      ctx.attribute(key("user"), user);
    }
    return ctx;
  }

  private String key(String key) {
    return scope == null ? key : scope + "." + key;
  }
}
