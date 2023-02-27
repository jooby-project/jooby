/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/attr")
public class MvcAttributes {

  @GET
  @Path("/secured/subpath")
  @Role(value = "Admin", level = "two")
  public String subpath() {
    return "Got it!!";
  }

  @GET
  @Path("/secured/otherpath")
  @Role("User")
  public String otherpath() {
    return "OK!";
  }
}
