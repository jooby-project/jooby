package source;

import io.jooby.Context;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

@Path("/inherited")
public class Controller1552 extends Controller1552Base {
  @GET(path = "/childOnly")
  public String childOnly(Context ctx) {
    return ctx.getRequestPath();
  }

  @POST(path = "/childOnly")
  public String childOnly_Post(Context ctx) {
    return ctx.getRequestPath();
  }
}
