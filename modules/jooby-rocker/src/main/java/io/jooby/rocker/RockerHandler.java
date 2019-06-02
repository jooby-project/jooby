/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.runtime.ArrayOfByteArraysOutput;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;

import javax.annotation.Nonnull;

class RockerHandler implements Route.Handler {
  private final Route.Handler next;

  public RockerHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    try {
      RockerModel template = (RockerModel) next.apply(ctx);
      ArrayOfByteArraysOutput buff = template.render(ArrayOfByteArraysOutput.FACTORY);
      ctx.setResponseType(MediaType.html);
      ctx.setResponseLength(buff.getByteLength());
      ctx.send(buff.asReadableByteChannel());
      return ctx;
    } catch (Throwable x) {
      ctx.sendError(x);
      return x;
    }
  }
}
