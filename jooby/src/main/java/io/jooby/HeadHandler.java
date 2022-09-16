/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Add support for HTTP Head requests.
 *
 * Usage:
 * <pre>{@code
 *   decorator(new HeadHandler());
 *
 *   get("/some", ctx -> "...");
 *
 * }</pre>
 * @author edgar
 * @since 2.0.4
 */
public class HeadHandler implements Route.Decorator {
  @NonNull @Override public Route.Handler apply(@NonNull Route.Handler next) {
    // NOOP, but we need it for marking the route as HTTP HEAD
    return ctx -> next.apply(ctx);
  }

  @NonNull @Override public void setRoute(@NonNull Route route) {
    route.setHttpHead(true);
  }
}
