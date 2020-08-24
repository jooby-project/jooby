/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.FileDownload;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class SendAttachment implements LinkedHandler {
  private Route.Handler next;

  public SendAttachment(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Object result = next.apply(ctx);
      if (ctx.isResponseStarted()) {
        return result;
      }
      return ctx.send(((FileDownload) result));
    } catch (Throwable x) {
      return ctx.sendError(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
