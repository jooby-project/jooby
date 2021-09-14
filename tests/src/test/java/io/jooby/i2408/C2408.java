package io.jooby.i2408;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
