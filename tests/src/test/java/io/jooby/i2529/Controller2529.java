/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2529;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.Produces;

public class Controller2529 {
  @GET
  @Path("/2529")
  @Produces("text/plain")
  public String hello() {
    return "Hello world";
  }
}
