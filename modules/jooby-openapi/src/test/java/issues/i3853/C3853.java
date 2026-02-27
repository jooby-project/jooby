/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3853;

import java.util.List;
import java.util.Optional;

import io.jooby.annotation.*;

@Path("/3853")
public class C3853 {

  @GET("/{id}")
  @Project("(id, name)")
  public U3853 findUser(@PathParam String id) {
    return null;
  }

  @GET(value = "/", projection = "(id, name)")
  public List<U3853> findUsers() {
    return null;
  }

  @GET("/optional")
  @Project("(id)")
  public Optional<U3853> findUserIdOnly() {
    return null;
  }

  @GET("/full-address/{id}")
  @Project("(id, address(*))")
  public U3853 userIdWithFullAddress(@PathParam String id) {
    return null;
  }

  @GET("/partial-address/{id}")
  @Project("(id, address(city))")
  public U3853 userIdWithAddressCity(@PathParam String id) {
    return null;
  }
}
