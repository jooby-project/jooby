/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3525;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;
import io.swagger.v3.oas.annotations.Parameter;

public class C3525 {
  @GET("/3525")
  @Parameter(name = "paramA", description = "paramA")
  @Parameter(name = "paramB", description = "paramB")
  public void repeatable(@QueryParam String paramA, @QueryParam String paramB) {}
}
