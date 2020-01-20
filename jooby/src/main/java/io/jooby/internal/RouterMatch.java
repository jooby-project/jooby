/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Router;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RouterMatch implements Router.Match {

  boolean matches;

  private Route route;

  private Map vars = Collections.EMPTY_MAP;

  private Route.Handler handler;

  public RouterMatch(Route route) {
    this.route = route;
    this.matches = true;
  }

  public RouterMatch() {
  }

  public void key(List<String> keys) {
    for (int i = 0; i < Math.min(keys.size(), vars.size()); i++) {
      vars.put(keys.get(i), vars.remove(i));
    }
  }

  public void value(String value) {

  }

  public void value(Chi.ZeroCopyString value) {
    if (vars == Collections.EMPTY_MAP) {
      vars = new LinkedHashMap();
    }
    vars.put(vars.size(), value.toString());
  }

  public void pop() {
    vars.remove(vars.size() - 1);
  }

  public void methodNotAllowed(Set<String> allow) {
    String allowString = allow.stream().collect(Collectors.joining(","));
    Route.Decorator decorator = next -> ctx -> {
      ctx.setResponseHeader("Allow", allowString);
      return next.apply(ctx);
    };
    handler = decorator.then(Route.METHOD_NOT_ALLOWED);
  }

  @Override public boolean matches() {
    return matches;
  }

  @Override public Route route() {
    return route;
  }

  @Override public Map<String, String> pathMap() {
    return vars;
  }

  public RouterMatch found(Route route) {
    this.route = route;
    this.matches = true;
    return this;
  }

  public void execute(Context context) {
    context.setPathMap(vars);
    context.setRoute(route);
    try {
      route.getPipeline().apply(context);
    } catch (Throwable x) {
      context.sendError(x);
    } finally {
      this.handler = null;
      this.route = null;
      this.vars = null;
    }
  }

  public RouterMatch missing(String method, String path, MessageEncoder encoder) {
    Route.Handler h;
    if (this.handler == null) {
      h = path.endsWith("/favicon.ico") ? Route.FAVICON : Route.NOT_FOUND;
    } else {
      h = this.handler;
    }
    this.route = new Route(method, path, h);
    this.route.setEncoder(encoder);
    this.route.setReturnType(Context.class);
    return this;
  }
}
