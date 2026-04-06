/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3863;

import io.jooby.annotation.trpc.Trpc;

@Trpc("users")
public class OverloadTrpc {

  @Trpc.Query
  public String ping() {
    return null;
  }

  @Trpc.Query("ping.since")
  public String ping(Integer since) {
    return null;
  }
}
