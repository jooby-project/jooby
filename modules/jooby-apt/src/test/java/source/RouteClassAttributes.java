package source;

import io.jooby.annotations.GET;

@RoleAnnotation("User")
public class RouteClassAttributes {
  @RoleAnnotation("Admin")
  @GET("/admin")
  public String admin() {
    return "...";
  }

  @GET("/user")
  public String user() {
    return "...";
  }
}
