/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.annotations.GET;

public class SubController extends BaseController {

  @GET("/subPath")
  public String subPath() {
    return "subPath";
  }
}
