/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class SendByteBuf implements LinkedHandler {
  private Route.Handler next;

  public SendByteBuf(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      ByteBuf result = (ByteBuf) next.apply(ctx);
      return ctx.send(result);
    } catch (Throwable x) {
      return ctx.sendError(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
