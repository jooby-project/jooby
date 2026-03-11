/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3864;

import io.jooby.Context;
import io.jooby.annotation.JsonRpc;

@JsonRpc("movies")
public class WithContext {

  public String rpcMethod1(Context ctx, Integer value) {
    return "1";
  }
}
