/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.QueryParam;

@Path("/base")
public abstract class BaseController {

  @GET
  public String base() {
    return "base";
  }

  @GET("/withPath")
  public String withPath(@QueryParam String q) {
    return "withPath";
  }
}
