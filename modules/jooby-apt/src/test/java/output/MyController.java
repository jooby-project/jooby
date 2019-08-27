package output;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.QueryParam;

import java.util.Map;

@Path("/")
public class MyController {

  @GET
  public Integer doIt(@QueryParam Integer x) {
    return x;
  }
}
