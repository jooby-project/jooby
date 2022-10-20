/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.Router;

public class RouteImportReferences extends Jooby {

  {
    Router routeA = new RouteA();
    use(routeA);

    use("/require", require(RouteA.class));

    Router route2 = new RouteA();
    use("/prefix", route2);
  }
}
