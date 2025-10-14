/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3786;

import io.jooby.Context;
import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;

@Path("/overrideMethod")
public class D3786 extends Base3786 {
  @GET("/childOnly")
  public String childOnly(Context ctx) {
    return ctx.getRequestPath();
  }

  @POST("/user")
  public String newPath(Context q) {
    return "/overrideMethod/user";
  }
}
