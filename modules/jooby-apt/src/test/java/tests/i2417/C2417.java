/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2417;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

public class C2417 {

  @io.swagger.v3.oas.annotations.Operation
  @GET("/2417")
  public String i2417(@QueryParam String name) {
    return name;
  }
}
