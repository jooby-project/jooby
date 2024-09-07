/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3397;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/")
public class Controller3397 {

  @GET("/welcome")
  public String sayHi() {
    return "hi";
  }
}
