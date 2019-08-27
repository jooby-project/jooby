package source;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

public class NoPathRoute {

  @GET
  public String root() {
    return "root";
  }

  @GET
  @Path("/subpath")
  public String subpath() {
    return "subpath";
  }
}
