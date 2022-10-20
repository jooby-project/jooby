/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2403;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.QueryParam;

public class Controller2403Copy {
  @GET
  @Path("copy")
  public String copy(@QueryParam String user) {
    return user;
  }
}
