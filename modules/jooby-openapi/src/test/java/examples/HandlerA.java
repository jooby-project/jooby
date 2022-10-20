/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;

public class HandlerA implements Route.Handler {
  @NonNull @Override
  public Object apply(@NonNull Context ctx) throws Exception {
    return null;
  }
}
