/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class CustomRouterName {

  @GET("/hello")
  public String hello(@QueryParam String name) {
    return name;
  }
}
