/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

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
  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    // NOOP, but we need it for marking the route as HTTP HEAD
    return ctx -> next.apply(ctx);
  }

  @Nonnull @Override public Route.Decorator setRoute(@Nonnull Route route) {
    route.setHttpHead(true);
    return this;
  }
}
