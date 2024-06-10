/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3422;

import io.jooby.annotation.GET;

public class C3422 {

  @GET("/3422")
  public ReactiveType reactiveType() {
    return new ReactiveType();
  }
}
