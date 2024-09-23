/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3539;

import io.jooby.annotation.GET;

public class C3539 {
  @GET("/3539")
  @Secured3525
  public void secured() {}
}
