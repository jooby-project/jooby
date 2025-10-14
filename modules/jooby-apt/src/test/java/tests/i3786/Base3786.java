/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3786;

import io.jooby.Context;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;

@Path("/base")
public abstract class Base3786 {

  @GET
  public String base() {
    return "base";
  }

  @GET("/withPath")
  public String withPath(@QueryParam String q) {
    return "withPath";
  }

  @GET("/{id}")
  public String getOne(Context ctx) {
    return "base: " + ctx.path("id").value();
  }
}
