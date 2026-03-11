/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3863;

import io.jooby.annotation.*;

@Trpc("users")
public class MixedMutation {

  @Trpc
  @POST
  public U3863 createUser(U3863 payload) {
    return null;
  }

  @Trpc
  @PUT
  public U3863 updateUser(U3863 payload) {
    return null;
  }

  @Trpc
  @PATCH
  public U3863 patchUser(U3863 payload) {
    return null;
  }

  @Trpc
  @DELETE
  public void deleteUser(U3863 payload) {}
}
