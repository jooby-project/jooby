/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.*;

import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Router;

public class RouterMatch implements Router.Match {

  private boolean matches;
  private Route route;
  private Route.Handler handler;

  private static final int INITIAL_CAPACITY = 5;
  private String[] keys = new String[INITIAL_CAPACITY];
  private String[] values = new String[INITIAL_CAPACITY];
  private int size = 0;

  public RouterMatch() {}

  public void key(List<String> routeKeys) {
    int limit = Math.min(routeKeys.size(), this.size);
    for (int i = 0; i < limit; i++) {
      this.keys[i] = routeKeys.get(i);
    }
  }

  public void value(String value) {
    if (size == values.length) {
      values = Arrays.copyOf(values, values.length * 2);
      keys = Arrays.copyOf(keys, keys.length * 2);
    }
    this.values[size++] = value;
  }

  public void pop() {
    if (this.size > 0) {
      this.size--;
    }
  }

  public void truncate(int newSize) {
    this.size = newSize;
  }

  public int size() {
    return this.size;
  }

  public void methodNotAllowed(Iterable<String> allow) {
    String allowString = String.join(",", allow);
    Route.Filter filter =
        next ->
            ctx -> {
              ctx.setResponseHeader("Allow", allowString);
              return next.apply(ctx);
            };
    handler = filter.then(Route.METHOD_NOT_ALLOWED);
  }

  @Override
  public boolean matches() {
    return matches;
  }

  @Override
  public Route route() {
    return route;
  }

  @Override
  public Map<String, String> pathMap() {
    if (size == 1) {
      return Collections.singletonMap(keys[0], values[0]);
    } else {
      int capacity = (int) (size / 0.75f) + 1;
      var map = new LinkedHashMap<String, String>(capacity);
      for (int i = 0; i < size; i++) {
        map.put(keys[i], values[i]);
      }
      return map;
    }
  }

  public RouterMatch found(Route route) {
    this.route = route;
    this.matches = true;
    return this;
  }

  @Override
  public Object execute(Context context, Route.Handler pipeline) {
    context.setPathMap(pathMap());
    context.setRoute(route);
    try {
      return pipeline.apply(context);
    } catch (Throwable x) {
      context.sendError(x);
      return x;
    } finally {
      this.handler = null;
      this.route = null;
      this.keys = null;
      this.values = null;
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
    return this;
  }
}
