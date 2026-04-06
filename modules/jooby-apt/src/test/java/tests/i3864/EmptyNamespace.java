/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3864;

import io.jooby.annotation.jsonrpc.JsonRpc;

@JsonRpc
public class EmptyNamespace {
  public String rpcMethod1() {
    return "1";
  }

  public String rpcMethod2() {
    return "1";
  }
}
