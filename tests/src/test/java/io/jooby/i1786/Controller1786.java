package io.jooby.i1786;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

import javax.annotation.Nonnull;
import java.util.UUID;

public class Controller1786 {
  @GET("/1786/nonnull")
  public UUID followNonnullAnnotation(@QueryParam @Nonnull UUID key) {
    return key;
  }
}
