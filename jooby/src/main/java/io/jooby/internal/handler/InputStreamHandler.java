package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;
import java.io.InputStream;

public class InputStreamHandler implements ChainedHandler {
  private Route.Handler next;

  public InputStreamHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    try {
      InputStream stream = (InputStream) next.apply(ctx);
      ctx.sendStream(stream);
    } catch (Throwable x) {
      ctx.sendError(x);
    }
    return ctx;
  }

  @Override public Route.Handler next() {
    return next;
  }
}
