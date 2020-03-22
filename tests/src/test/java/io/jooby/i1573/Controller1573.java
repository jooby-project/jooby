package io.jooby.i1573;

import io.jooby.annotations.GET;
import io.jooby.annotations.PathParam;

import java.util.Optional;

public class Controller1573 {

  @GET("/profile/{id}?")
  public String profile(@PathParam Optional<String> id) {
    return id.orElse("self");
  }
}
