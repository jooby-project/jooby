/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3507;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C3507 {
  @GET("/3507")
  @Nullable public String get(@QueryParam @NonNull String query) {
    return null;
  }
}
