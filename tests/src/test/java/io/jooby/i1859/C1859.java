/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1859;

import java.util.Optional;

import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

@Path(("/c"))
public class C1859 {
  @POST("/i1859")
  public String foo(String theBodyParam) {
    return Optional.ofNullable(theBodyParam).orElse("empty");
  }
}
