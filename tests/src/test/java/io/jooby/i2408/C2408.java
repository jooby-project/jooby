/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2408;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C2408 {
  @GET("/2408/nonnull")
  public String nonnull(@NonNull @QueryParam String name) {
    return name;
  }

  @GET("/2408/nullable")
  public String nullable(@Nullable @QueryParam String name, @QueryParam String blah) {
    if (name == null) {
      return "nothing";
    }
    return name;
  }
}
