package io.jooby.i1792;

import io.jooby.StatusCode;
import io.jooby.annotations.POST;

public class Controller1792 {

  @POST("/c/1792")
  public StatusCode create() {
    return StatusCode.CREATED;
  }
}
