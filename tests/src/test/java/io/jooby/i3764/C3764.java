/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3764;

import io.jooby.Context;
import io.jooby.annotation.GET;
import jakarta.inject.Inject;

public class C3764 {

  private final Context ctx;

  @Inject
  public C3764(Context ctx) {
    this.ctx = ctx;
  }

  @GET("/3764")
  public String path() {
    return ctx.getRequestPath();
  }
}
