/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2408;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

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
