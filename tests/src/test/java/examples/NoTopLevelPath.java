package examples;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

public class NoTopLevelPath {
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
