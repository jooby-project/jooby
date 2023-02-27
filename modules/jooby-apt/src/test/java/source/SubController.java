/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.annotation.GET;

public class SubController extends BaseController implements SomeInterface {

  @GET("/subPath")
  public String subPath() {
    return "subPath";
  }
}
