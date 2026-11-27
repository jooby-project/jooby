/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import java.util.Set;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("test")
public class Controller4045 {
  @GET
  public byte[] test(Set<Integer> payload) {
    return new byte[0];
  }
}
