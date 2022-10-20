/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package output;

import io.jooby.StatusCode;
import io.jooby.annotations.GET;

public class MyController {

  @GET("/default")
  public StatusCode controllerMethod() {
    return StatusCode.CREATED;
  }
}
