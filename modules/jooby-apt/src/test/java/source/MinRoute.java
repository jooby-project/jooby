package source;

import io.jooby.annotations.GET;

public class MinRoute {
  @GET("/mypath")
  public String controllerMethod() {
    return "/mypath";
  }
}

