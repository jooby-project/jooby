/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.metrics;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.internal.metrics.NoCacheHeader;

import javax.annotation.Nonnull;

public class PingHandler implements Route.Handler {

  @Nonnull
  @Override
  public Object apply(@Nonnull Context ctx) {
    ctx.setResponseCode(StatusCode.OK);
    ctx.setResponseType(MediaType.text);
    NoCacheHeader.add(ctx);
    return "pong";
  }
}
