package examples;

import io.jooby.annotations.GET;

public class SubController extends BaseController {

  @GET("/subPath")
  public String subPath() {
    return "subPath";
  }
}
