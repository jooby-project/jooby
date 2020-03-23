package examples;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.QueryParam;

@Path("/overrideMethod")
public class OverrideMethodSubClassController extends BaseController {

  @Override public String base() {
    return super.base();
  }

  @GET("/newpath")
  @Override public String withPath(@QueryParam String q) {
    return super.withPath(q);
  }
}
