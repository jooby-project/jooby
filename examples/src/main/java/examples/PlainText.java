/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;

import java.util.Optional;

public class PlainText {
  @GET
  @Path("/plaintext")
  public String plainText(@PathParam Optional<String> message) {
    return message.orElse("Hello, World!");
  }
}
