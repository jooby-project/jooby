/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import java.util.List;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class CustomRouterName {

  @GET("/hello")
  public List<? super String> hello(@QueryParam String name) {
    return null;
  }
}
