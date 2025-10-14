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

@Path("/inherited")
public class C3786 extends Base3786 {
  @GET("/childOnly")
  public String childOnly(Context ctx) {
    return ctx.getMethod() + ctx.getRequestPath();
  }

  @POST("/childOnly")
  public String childOnlyPost(Context ctx) {
    return ctx.getMethod() + ctx.getRequestPath();
  }
}
