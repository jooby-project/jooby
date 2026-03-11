/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3864;

import io.jooby.annotation.JsonRpc;
import jakarta.inject.Inject;

@JsonRpc
public class DIService {

  @Inject
  public DIService(CustomNaming value) {}

  public String rpcMethod1() {
    return "1";
  }
}
