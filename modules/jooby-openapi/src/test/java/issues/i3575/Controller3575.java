/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3575;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.swagger.v3.oas.annotations.Hidden;

@Path("/3575")
public class Controller3575 {

  @GET("/mvc")
  @Hidden
  public String sayHi() {
    return "hi";
  }
}
