/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2352;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.annotation.FormParam;
import io.jooby.annotation.POST;

public class C2352 {
  @POST("/2352/nonnull")
  public String nonnull(@NonNull @FormParam String name) {
    return name;
  }

  @POST("/2352/nullable")
  public String nullable(@Nullable @FormParam String name) {
    return String.valueOf(name);
  }
}
