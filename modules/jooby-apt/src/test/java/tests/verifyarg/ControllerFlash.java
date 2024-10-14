/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.verifyarg;

import io.jooby.annotation.FlashParam;
import io.jooby.annotation.GET;

class ControllerFlash {
  @GET
  public void param(@FlashParam Object param) {}
}
