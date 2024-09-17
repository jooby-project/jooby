/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.verifyarg;

import io.jooby.annotation.GET;
import jakarta.ws.rs.HeaderParam;

class ControllerHeader {
  @GET
  public void param(@HeaderParam("test") Object param) {
  }
}
