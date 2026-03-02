/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3863;

import io.jooby.annotation.*;

@Trpc("users")
public class C3863 {

  @Trpc.Query
  public U3863 findUser(@PathParam long id) {
    return null;
  }

  //  @Trpc.Mutation
  //  public U3863 updateUser(@PathParam String id, U3863 payload) {
  //    return null;
  //  }
}
