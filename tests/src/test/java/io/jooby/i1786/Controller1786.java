/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1786;

import java.util.UUID;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

public class Controller1786 {
  @GET("/1786/nonnull")
  public UUID followNonnullAnnotation(@QueryParam @NonNull UUID key) {
    return key;
  }
}
