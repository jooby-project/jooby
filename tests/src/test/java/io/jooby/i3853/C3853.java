/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3853;

import java.util.List;
import java.util.Optional;

import io.jooby.Projected;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.Project;

@Path("/3853")
public class C3853 {
  @GET(value = "/stub", projection = "(id, name)")
  public U3853 projectUser() {
    return U3853.createUser();
  }

  @GET("/optional")
  @Project("(id, name)")
  public Optional<U3853> findUser() {
    return Optional.of(U3853.createUser());
  }

  @GET("/list")
  @Project("(id)")
  public List<U3853> findUsers() {
    return List.of(U3853.createUser());
  }

  @GET("/projected")
  @Project("(id, name)")
  public Projected<U3853> projected() {
    return Projected.wrap(U3853.createUser()).include("(id, name)");
  }

  @GET(value = "/projectedProjection", projection = "(id, name)")
  public Projected<U3853> projectedProjection() {
    return Projected.wrap(U3853.createUser()).include("(id, name)");
  }
}
