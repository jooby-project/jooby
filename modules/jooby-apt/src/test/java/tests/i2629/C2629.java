/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2629;

import java.util.List;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C2629 {
  @GET("/2629")
  public String queryUsers(
      @QueryParam String type, @QueryParam List<Integer> number, @QueryParam boolean bool) {
    return type + ":" + number + ":" + bool;
  }
}
