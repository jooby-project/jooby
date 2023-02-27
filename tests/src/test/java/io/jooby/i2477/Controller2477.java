/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2477;

import io.jooby.StatusCode;
import io.jooby.annotation.POST;
import io.jooby.annotation.PUT;
import io.jooby.annotation.Path;

public class Controller2477 {
  @PUT
  @Path("/2477")
  public void doPut() {}

  @POST
  @Path("/2477")
  public StatusCode doPost() {
    return StatusCode.CREATED;
  }
}
