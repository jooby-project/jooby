package io.jooby.i2352;

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import io.jooby.annotations.FormParam;
import io.jooby.annotations.POST;

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
