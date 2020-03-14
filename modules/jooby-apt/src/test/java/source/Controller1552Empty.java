package source;

import io.jooby.Context;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/inherit_empty")
public class Controller1552Empty extends Controller1552Base {
  @GET
  public String fake(Context ctx) {
    return ctx.getRequestPath();
  }
}
