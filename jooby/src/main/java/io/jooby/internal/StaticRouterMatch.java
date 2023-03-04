/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class StaticRouterMatch implements Router.Match {
  private final Route route;

  public StaticRouterMatch(Route route) {
    this.route = route;
  }

  @Override public boolean matches() {
    return true;
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  @Override public void execute(@NotNull Context context, @NotNull Route.Handler pipeline) {
    context.setRoute(route);
    try {
      pipeline.apply(context);
    } catch (Throwable x) {
      context.sendError(x);
    }
  }

  @Nonnull @Override public Map<String, String> pathMap() {
    return Collections.emptyMap();
  }
}
