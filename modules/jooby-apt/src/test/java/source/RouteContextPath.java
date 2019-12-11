package source;

import io.jooby.Route;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/")
public class RouteContextPath {

  @GET
  public Route route(Route route) {
    return route;
  }

  @GET("/subpath")
  public Route subpath(Route route) {
    return route;
  }
}
