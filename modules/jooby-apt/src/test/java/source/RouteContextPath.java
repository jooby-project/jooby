/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.Route;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/")
public class RouteContextPath {

  @GET
  public String route(Route route) {
    return route.getPattern();
  }

  @GET("/subpath")
  public String subpath(Route route) {
    return route.getPattern();
  }
}
