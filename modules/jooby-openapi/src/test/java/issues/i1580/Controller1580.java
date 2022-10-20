/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1580;

import io.jooby.annotations.PUT;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;

@Path("/pets")
public class Controller1580 {
  @PUT
  @Path("/{id}")
  public Data1580 updatePet(Data1580 body, @PathParam String id) { // -> leads to NPE
    return body;
  }
}
