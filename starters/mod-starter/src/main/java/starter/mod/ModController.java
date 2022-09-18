package starter.mod;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/mod")
public class ModController {
  
  @GET("")
  public String hello() {
    return "hello";
  }

}
