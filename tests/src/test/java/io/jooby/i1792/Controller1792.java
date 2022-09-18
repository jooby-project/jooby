package io.jooby.i1792;

import io.jooby.annotations.POST;
import io.jooby.StatusCode;

public class Controller1792 {

  @POST("/c/1792")
  public StatusCode create() {
    return StatusCode.CREATED;
  }
}
