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
  public String ping(Integer year) {
    return null;
  }

  //  @Trpc.Query
  //  public String ping() {
  //    return null;
  //  }
  //
  //  @Trpc.Query
  //  public void clear() {
  //
  //  }
  //
  //  @Trpc.Query
  //  public U3863 findUser(Context ctx, @PathParam long id) {
  //    return null;
  //  }
  //
  //  @Trpc.Query
  //  public List<U3863> multipleSimpleArgs(String q, byte type) {
  //    return null;
  //  }
  //
  //  @Trpc.Query
  //  public List<U3863> multipleComplexArguments(U3863 current, List<U3863> users) {
  //    return null;
  //  }
  //
  //  @Trpc.Mutation
  //  public U3863 updateUser(String id, U3863 payload) {
  //    return null;
  //  }

}
