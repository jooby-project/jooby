package tests.i2026;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/api/todo")
public class C2026 {

  @GET
  public String handle() {
    return "TODO...";
  }
}
