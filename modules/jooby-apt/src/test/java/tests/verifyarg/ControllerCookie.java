/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.verifyarg;

import io.jooby.annotation.CookieParam;
import io.jooby.annotation.GET;

class ControllerCookie {
  @GET
  public void param(@CookieParam Object param) {
  }
}
