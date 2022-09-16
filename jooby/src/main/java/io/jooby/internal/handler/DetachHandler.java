/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import edu.umd.cs.findbugs.annotations.NonNull;

public class DetachHandler implements LinkedHandler {
  private final Route.Handler next;

  public DetachHandler(Route.Handler next) {
    this.next = next;
  }

  @NonNull @Override public Object apply(@NonNull Context ctx) throws Exception {
    return ctx.detach(next);
  }

  @Override public Route.Handler next() {
    return next;
  }
}
