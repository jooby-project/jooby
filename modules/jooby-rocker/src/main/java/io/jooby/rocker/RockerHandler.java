/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.runtime.StringBuilderOutput;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;

import javax.annotation.Nonnull;

class RockerHandler implements Route.Handler {
  private final Route.Handler next;

  RockerHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    try {
      RockerModel template = (RockerModel) next.apply(ctx);
      ctx.setResponseType(MediaType.html);
      ctx.send(template.render(StringBuilderOutput.FACTORY).toString());
      return ctx;
    } catch (Throwable x) {
      ctx.sendError(x);
      return x;
    }
  }
}
