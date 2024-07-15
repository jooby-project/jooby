/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import io.jooby.Context;
import io.jooby.annotation.BindParam;
import io.jooby.annotation.GET;

public class C3472d {

  @GET("/3472/bean-controller")
  public BindController bind(@BindParam BindController bean) {
    return bean;
  }

  public static BindController create(Context ctx) {
    return new BindController("bean-controller-static:" + ctx.query("value").value());
  }
}
