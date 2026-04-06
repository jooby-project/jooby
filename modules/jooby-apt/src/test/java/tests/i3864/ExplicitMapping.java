/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3864;

import io.jooby.annotation.jsonrpc.JsonRpc;

@JsonRpc("explicit")
public class ExplicitMapping {

  @JsonRpc("onlyThis")
  public String rpcMethod1() {
    return "1";
  }

  public String rpcMethod2() {
    return rpcMethod3();
  }

  private String rpcMethod3() {
    return "1";
  }
}
