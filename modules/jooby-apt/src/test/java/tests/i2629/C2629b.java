/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2629;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jooby.Context;
import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C2629b {

  @GET("/2629")
  public String mix(
      @QueryParam String s,
      @QueryParam Integer i,
      @QueryParam double d,
      Context ctx,
      @QueryParam long j,
      @QueryParam Float f,
      @QueryParam boolean b) {
    return Stream.of(s, i, d, ctx.getMethod(), j, f, b)
        .map(Objects::toString)
        .collect(Collectors.joining("/"));
  }
}
