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
import io.jooby.internal.handler.DefaultHandler;

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

  /** Default constructor. */
  public HeadHandler() {}

  @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      if (ctx.getMethod().equals(Router.HEAD)) {
        return DefaultHandler.DEFAULT.apply(next).apply(new HeadContext(ctx));
      } else {
        return next.apply(ctx);
      }
    };
  }

  @Override
  public void setRoute(@NonNull Route route) {
    route.setHttpHead(true);
  }
}
