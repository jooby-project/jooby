/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3059;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/mvc/a")
public class ControllerA3059 {
  @GET
  public String pathA() {
    return "Hello!";
  }
}
