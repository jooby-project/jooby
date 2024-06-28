/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;

public class DetachHandler implements Route.Filter {

  public static final DetachHandler DETACH = new DetachHandler();

  private DetachHandler() {}

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> ctx.detach(next);
  }

  @Override
  public String toString() {
    return "detach";
  }
}
