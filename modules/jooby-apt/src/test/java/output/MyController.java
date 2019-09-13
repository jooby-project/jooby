package output;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.jooby.annotations.QueryParam;

import java.util.Map;

@Path("/")
public class MyController {

  @GET
  public String doIt(@PathParam("p1") String p1) {
    return p1;
  }
}
