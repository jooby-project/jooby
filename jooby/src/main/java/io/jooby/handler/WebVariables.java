/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;

/**
 * Add common variables to as {@link Context} attributes so they are accessible from template
 * engine.
 *
 * <p>Usage:
 *
 * <pre>{@code
 *  use(new WebVariables());
 *
 * get("/", ctx -> new ModelAndView("index.ftl"));
 * }</pre>
 *
 * Template engine will be able to access to the following attributes:
 *
 * <p>- contextPath. Empty when context path is set to <code>/</code> or actual context path. -
 * path. Current request path, as defined by {@link Context#getRequestPath()}. - user. Current user
 * (if any) as defined by {@link Context#getUser()}.
 *
 * @author edgar
 * @since 2.4.0
 */
public class WebVariables implements Route.Filter {

  private final String scope;

  /**
   * Creates a web variables under the given scope.
   *
   * @param scope Scope to use.
   */
  public WebVariables(@NonNull String scope) {
    this.scope = scope;
  }

  /** Creates a new web variables. */
  public WebVariables() {
    this.scope = null;
  }

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> next.apply(webvariables(ctx));
  }

  private Context webvariables(Context ctx) {
    String contextPath = ctx.getContextPath();
    ctx.setAttribute(key("contextPath"), contextPath.equals("/") ? "" : contextPath);
    ctx.setAttribute(key("path"), ctx.getRequestPath());
    Object user = ctx.getUser();
    if (user != null) {
      ctx.setAttribute(key("user"), user);
    }
    return ctx;
  }

  private String key(String key) {
    return scope == null ? key : scope + "." + key;
  }
}
