/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3854;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.Project;
import tests.i3804.Base3804;

@Path("/3854")
public class C3854 extends Base3804 {
  @GET()
  @Project("(id, name)")
  public U3854 projectUser() {
    return new U3854(1, "Projected User", "Projected", "User");
  }
}
