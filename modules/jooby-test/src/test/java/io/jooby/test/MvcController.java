/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import io.jooby.Context;
import io.jooby.annotations.Dispatch;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

@Path("/mvc")
public class MvcController {

  @GET
  public String getIt(Context ctx) {
    return ctx.getRequestPath();
  }

  @Dispatch("single")
  @GET("/single")
  public String single() {
    return Thread.currentThread().getName();
  }

  @POST
  public PojoBody post(PojoBody body) {
    return body;
  }
}
