/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3490;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C3490 {

  @GET("/3490")
  public Box3490<Integer> get(@QueryParam int id) {
    return null;
  }
}
