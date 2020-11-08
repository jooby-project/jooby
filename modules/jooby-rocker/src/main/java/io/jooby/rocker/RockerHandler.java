/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import javax.annotation.Nonnull;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerOutputFactory;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;

class RockerHandler implements Route.Handler {
  private final Route.Handler next;

  private final RockerOutputFactory<ByteBufferOutput> factory;

  RockerHandler(Route.Handler next, RockerOutputFactory<ByteBufferOutput> factory) {
    this.next = next;
    this.factory = factory;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      RockerModel template = (RockerModel) next.apply(ctx);
      ctx.setResponseType(MediaType.html);
      return ctx.send(template.render(factory).toBuffer());
    } catch (Throwable x) {
      ctx.sendError(x);
      return x;
    }
  }
}
