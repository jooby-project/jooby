/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerOutputFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.MediaType;
import io.jooby.Route;

class RockerHandler implements Route.Filter {
  private final RockerOutputFactory<DataBufferOutput> factory;

  RockerHandler(RockerOutputFactory<DataBufferOutput> factory) {
    this.factory = factory;
  }

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      try {
        RockerModel template = (RockerModel) next.apply(ctx);
        ctx.setResponseType(MediaType.html);
        return ctx.send(template.render(factory).toBuffer());
      } catch (Throwable x) {
        ctx.sendError(x);
        return x;
      }
    };
  }
}
