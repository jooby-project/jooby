/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3863;

import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;
import io.jooby.trpc.jackson2.TrpcJackson2Module;

public class Jackson2TrpcProtocolTest extends AbstractTrpcProtocolTest {
  @Override
  protected void installJsonEngine(Jooby app) {
    app.install(new JacksonModule());
    app.install(new TrpcJackson2Module());
  }
}
