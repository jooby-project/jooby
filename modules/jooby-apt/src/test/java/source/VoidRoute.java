package source;

import io.jooby.annotations.DELETE;
import io.jooby.annotations.Path;

@Path("/void")
public class VoidRoute {

  @DELETE
  public void noContent() {

  }
}
