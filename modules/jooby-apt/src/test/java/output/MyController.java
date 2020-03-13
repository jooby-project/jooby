package output;

import io.jooby.Context;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.jooby.annotations.QueryParam;

import java.util.Map;

public class MyController {

  @GET("/default")
  public void controllerMethod() {
  }
}
