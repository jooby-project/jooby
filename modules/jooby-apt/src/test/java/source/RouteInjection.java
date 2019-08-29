package source;

import io.jooby.Route;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/route")
public class RouteInjection {

  @GET
  public Route route(Route route) {
    return route;
  }
}
