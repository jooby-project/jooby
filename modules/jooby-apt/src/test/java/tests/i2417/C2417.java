package tests.i2417;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

public class C2417 {

  @io.swagger.v3.oas.annotations.Operation
  @GET("/2417")
  public String i2417(@NonNull @QueryParam String name) {
    return name;
  }
}
