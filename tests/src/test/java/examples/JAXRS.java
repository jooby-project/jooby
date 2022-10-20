/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/jaxrs")
public class JAXRS {

  @GET
  public String getIt() {
    return "Got it!";
  }
}
