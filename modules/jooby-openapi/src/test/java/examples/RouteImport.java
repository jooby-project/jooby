/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;

public class RouteImport extends Jooby {

  {
    use(new RouteA());

    path(
        "/main",
        () -> {
          use(new RouteA());

          use("/submain", new RouteA());
        });

    use(new RouteA());

    use("/require", require(RouteA.class));

    use("/subroute", new RouteA());
  }
}
