/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3476;

import java.util.List;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C3476 {

  @GET("/3476")
  public <T> List<T> get(@QueryParam List<T> list) {
    return List.of();
  }

  @GET("/box")
  public Box<Integer> box(@QueryParam Box<Integer> box) {
    return box;
  }
}
