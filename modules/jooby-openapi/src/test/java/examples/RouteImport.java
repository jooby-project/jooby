/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;

public class RouteImport extends Jooby {

  {
    mount(new RouteA());

    path(
        "/main",
        () -> {
          mount(new RouteA());

          mount("/submain", new RouteA());
        });

    mount(new RouteA());

    mount("/require", require(RouteA.class));

    mount("/subroute", new RouteA());
  }
}
