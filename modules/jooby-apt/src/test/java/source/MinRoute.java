package source;

import io.jooby.annotations.GET;

public class MinRoute {
  @RoleAnnotation("User")
  @GET("/mypath")
  public String controllerMethod() {
    return "/mypath";
  }
}

