/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3853;

import java.util.List;
import java.util.Optional;

import io.jooby.Projected;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.Project;

@Path("/3854")
public class C3853 {
  @GET("/stub")
  @Project("(id, name)")
  public U3853 projectUser() {
    return new U3853(1, "Projected User", "Projected", "User");
  }

  @GET("/optinal")
  @Project("(id, name)")
  public Optional<U3853> findUser() {
    return Optional.of(new U3853(1, "Projected User", "Projected", "User"));
  }

  @GET("/list")
  @Project("(id, name)")
  public List<U3853> findUsers() {
    return List.of(new U3853(1, "Projected User", "Projected", "User"));
  }

  @GET("/list")
  @Project("(id, name)")
  public Projected<U3853> projected() {
    return Projected.wrap(new U3853(1, "Projected User", "Projected", "User"))
        .include("(id, name)");
  }
}
