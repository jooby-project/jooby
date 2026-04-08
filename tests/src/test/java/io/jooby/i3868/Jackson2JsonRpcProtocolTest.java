/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3868;

import io.jooby.Jooby;
import io.jooby.jackson.Jackson2Module;
import io.jooby.jsonrpc.jackson2.JsonRpcJackson2Module;

public class Jackson2JsonRpcProtocolTest extends AbstractJsonRpcProtocolTest {
  @Override
  protected void installJsonEngine(Jooby app) {
    app.install(new Jackson2Module());
    app.install(new JsonRpcJackson2Module());
  }
}
