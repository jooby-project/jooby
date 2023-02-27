/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

public class NoPathRoute {

  @GET
  public String root() {
    return "root";
  }

  @GET
  @Path("/subpath")
  public String subpath() {
    return "subpath";
  }
}
