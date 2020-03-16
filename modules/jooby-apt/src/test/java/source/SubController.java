package source;

import io.jooby.annotations.GET;

public class SubController extends BaseController implements SomeInterface {

  @GET("/subPath")
  public String subPath() {
    return "subPath";
  }
}
