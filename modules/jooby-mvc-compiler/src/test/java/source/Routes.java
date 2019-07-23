package source;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Path("/path")
public class Routes {

  @GET
  public String doIt(Context ctx) {
    Route route = ctx.getRoute();
    assertEquals(String.class, route.getReturnType());
    return ctx.pathString();
  }

  @GET("/subpath")
  public List<String> subpath(Context ctx) {
    Route route = ctx.getRoute();
    assertEquals(Reified.list(String.class), route.getReturnType());
    return Arrays.asList(ctx.pathString());
  }
}
