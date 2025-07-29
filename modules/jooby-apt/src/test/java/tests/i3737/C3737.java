/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3737;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/3737")
public class C3737 {

  public C3737() throws Exception {}

  @GET("/hello")
  public String hello() {
    return "hello world";
  }
}
