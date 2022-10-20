/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1573;

import java.util.Optional;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;

@Path("/c")
public class Controller1573 {

  @GET("/profile/{id}?")
  public String profile(@PathParam Optional<String> id) {
    return id.orElse("self");
  }
}
