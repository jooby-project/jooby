/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.AttachedFile;
import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class SendAttachment implements LinkedHandler {
  private Route.Handler next;

  public SendAttachment(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      AttachedFile file = (AttachedFile) next.apply(ctx);
      return ctx.send(file);
    } catch (Throwable x) {
      return ctx.sendError(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
