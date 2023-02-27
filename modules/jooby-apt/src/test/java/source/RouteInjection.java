/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.Route;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/route")
public class RouteInjection {

  @GET
  public Route route(Route route) {
    return route;
  }
}
