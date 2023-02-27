/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1292;

import io.jooby.annotation.GET;

public class Controller1292 {
  @GET
  public String response() {
    return "mvc";
  }
}
