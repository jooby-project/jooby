/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import io.jooby.Context;
import io.jooby.Route;

public interface MvcHandler extends Route.Handler {
  Object[] arguments(Context ctx);
}
