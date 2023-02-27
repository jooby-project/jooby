/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1601;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/1601")
public class Controller1601 {
  @GET
  public String doSomething() {
    return "..";
  }
}
