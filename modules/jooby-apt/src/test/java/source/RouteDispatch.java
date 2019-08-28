package source;

import io.jooby.annotations.Dispatch;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Dispatch
public class RouteDispatch {

  @Path("/toplevel")
  @GET
  public void toplevel() {

  }

  @Path("/methodlevel")
  @GET
  @Dispatch("single")
  public void methodlevel() {

  }
}
