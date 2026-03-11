/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3863;

import io.jooby.Context;
import io.jooby.annotation.Trpc;

@Trpc("users")
public class TopLevelTrpcDoesNothingByItSelf {

  public String ping(Integer year) {
    return null;
  }

  public U3863 findUser(Context ctx, long id) {
    return null;
  }
}
