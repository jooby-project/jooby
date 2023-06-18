/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.internal.HeadContext;

/**
 * Add support for HTTP Head requests.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * use(new HeadHandler());
 *
 * get("/some", ctx -> "...");
 *
 * }</pre>
 *
 * @author edgar
 * @since 2.0.4
 */
public class HeadHandler implements Route.Filter {
  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx ->
        ctx.getMethod().equals(Router.HEAD) ? next.apply(new HeadContext(ctx)) : next.apply(ctx);
  }

  @NonNull @Override
  public void setRoute(@NonNull Route route) {
    route.setHttpHead(true);
  }
}
