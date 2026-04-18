/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Context;
import io.jooby.Route;

public class HandlerA implements Route.Handler {
  @Override
  public Object apply(Context ctx) throws Exception {
    return null;
  }
}
