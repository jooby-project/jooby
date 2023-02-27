/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2405;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C2405 {
  @GET("/2405/blah")
  public String blah(@QueryParam Bean2405 blah) {
    return blah.toString();
  }

  @GET("/2405/blah2")
  public String blah2(@QueryParam Bean2405 blah) {
    return blah.toString();
  }
}
