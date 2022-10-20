/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
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
