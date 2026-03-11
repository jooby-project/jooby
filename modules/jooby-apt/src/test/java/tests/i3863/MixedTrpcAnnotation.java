/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3863;

import io.jooby.Context;
import io.jooby.annotation.*;

@Trpc("users")
@Path("/api/users")
public class MixedTrpcAnnotation {

  @Trpc
  @GET("/{id}")
  public U3863 getUserById(Context ctx, @PathParam long id) {
    return null;
  }

  @Trpc
  @POST
  public U3863 createUser(U3863 payload) {
    return null;
  }
}
