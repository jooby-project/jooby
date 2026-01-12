/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3836;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;
import tests.i3804.Base3804;

@Path("/3836")
public class C3836 extends Base3804 {
  @GET("/attribute")
  public String oddNameWithNameAttribute(@QueryParam(name = "some-http") String value) {
    return value;
  }
}
