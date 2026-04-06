/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3868;

import io.jooby.Jooby;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.jsonrpc.jackson3.JsonRpcJackson3Module;

public class Jackson3JsonRpcProtocolTest extends AbstractJsonRpcProtocolTest {
  @Override
  protected void installJsonEngine(Jooby app) {
    app.install(new Jackson3Module());
    app.install(new JsonRpcJackson3Module());
  }
}
