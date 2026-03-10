/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3863;

import io.jooby.Jooby;
import io.jooby.jackson3.Jackson3Module;

public class Jackson3TrpcProtocolTest extends AbstractTrpcProtocolTest {
  @Override
  protected void installJsonEngine(Jooby app) {
    app.install(new Jackson3Module());
  }
}
