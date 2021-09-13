package issues.i2403;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.QueryParam;

public class Controller2403 {
  @GET
  @Path("me")
  public String me(@QueryParam String user) {
    return user;
  }
}
