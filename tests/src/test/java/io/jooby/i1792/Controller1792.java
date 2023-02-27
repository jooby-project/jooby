/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1792;

import io.jooby.StatusCode;
import io.jooby.annotation.POST;

public class Controller1792 {

  @POST("/c/1792")
  public StatusCode create() {
    return StatusCode.CREATED;
  }
}
