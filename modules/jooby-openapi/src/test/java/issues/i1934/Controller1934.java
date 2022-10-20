/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1934;

import examples.Person;
import io.jooby.annotations.ContextParam;
import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/openapi")
public class Controller1934 {

  @GET
  @Path("me")
  public Person getUserInformation(@ContextParam Person user) {
    return user;
  }
}
