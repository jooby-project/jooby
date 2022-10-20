/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.annotations.GET;

public class MinRoute {
  @GET("/mypath")
  public String controllerMethod() {
    return "/mypath";
  }
}
