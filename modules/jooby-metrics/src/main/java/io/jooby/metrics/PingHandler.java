/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.metrics;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;

public class PingHandler implements Route.Handler {

  @NonNull @Override
  public Object apply(@NonNull Context ctx) {
    ctx.setResponseType(MediaType.text);
    ctx.setResponseHeader(MetricsModule.CACHE_HEADER_NAME, MetricsModule.CACHE_HEADER_VALUE);
    return "pong";
  }
}
