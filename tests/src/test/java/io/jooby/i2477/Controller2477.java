package io.jooby.i2477;

import io.jooby.StatusCode;
import io.jooby.annotations.POST;
import io.jooby.annotations.PUT;
import io.jooby.annotations.Path;

public class Controller2477 {
  @PUT
  @Path("/2477")
  public void doPut() {
  }

  @POST
  @Path("/2477")
  public StatusCode doPost() {
    return StatusCode.CREATED;
  }
}
