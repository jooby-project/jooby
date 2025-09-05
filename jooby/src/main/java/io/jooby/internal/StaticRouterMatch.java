/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.Collections;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;

public class StaticRouterMatch implements Router.Match {
  private final Route route;

  public StaticRouterMatch(Route route) {
    this.route = route;
  }

  @Override
  public boolean matches() {
    return true;
  }

  @NonNull @Override
  public Route route() {
    return route;
  }

  @Override
  public Object execute(@NonNull Context context, @NonNull Route.Handler pipeline) {
    context.setRoute(route);
    try {
      return pipeline.apply(context);
    } catch (Throwable x) {
      context.sendError(x);
      return x;
    }
  }

  @Override
  public Object execute(@NonNull Context context) {
    return execute(context, route.getPipeline());
  }

  @NonNull @Override
  public Map<String, String> pathMap() {
    return Collections.emptyMap();
  }
}
