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
