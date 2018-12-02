package io.jooby.internal.handler;

import io.jooby.Route;

public interface ChainedHandler extends Route.Handler {
  Route.Handler next();
}
