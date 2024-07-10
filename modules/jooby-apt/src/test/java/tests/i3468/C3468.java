/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3468;

import io.jooby.Context;
import io.jooby.annotation.GET;

public class C3468 {

  @GET("test")
  public String test(Context ctx) {
    return ctx.getRequestPath();
  }
}
