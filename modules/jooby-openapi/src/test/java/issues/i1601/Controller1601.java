package issues.i1601;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/1601")
public class Controller1601 {
  @GET
  public String doSomething() {
    return "..";
  }
}
