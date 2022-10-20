/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.annotations.DELETE;
import io.jooby.annotations.Path;

@Path("/void")
public class VoidRoute {

  @DELETE
  public void noContent() {}
}
