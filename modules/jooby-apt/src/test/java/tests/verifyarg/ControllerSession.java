/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.verifyarg;

import io.jooby.annotation.GET;
import io.jooby.annotation.SessionParam;

class ControllerSession {
  @GET
  public void param(@SessionParam Object param) {}
}
