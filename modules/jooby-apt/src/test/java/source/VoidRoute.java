package source;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/void")
public class VoidRoute {

  @GET
  public void noContent() {

  }
}
