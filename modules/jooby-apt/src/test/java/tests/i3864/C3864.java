/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3864;

import io.jooby.Context;
import io.jooby.annotation.JsonRpc;

@JsonRpc("users")
public class C3864 {

  @JsonRpc
  public String ping(Context ctx, int year) {
    return null;
  }
}
