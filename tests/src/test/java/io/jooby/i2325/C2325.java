/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2325;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

public class C2325 {
  @GET("/2325")
  public String getMy(@QueryParam MyID2325 id) {
    return id.toString();
  }
}
