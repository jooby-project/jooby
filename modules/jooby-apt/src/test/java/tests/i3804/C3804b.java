/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3804;

import io.jooby.Context;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import jakarta.inject.Inject;

@Path("/3804")
public class C3804b {
  @Inject protected Service3804 paramDecoder;

  @GET
  public Object list(Context ctx) {
    return paramDecoder; // paramDecoder is null
  }
}
