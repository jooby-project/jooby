/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3863;

import io.jooby.Jooby;
import io.jooby.avaje.jsonb.AvajeJsonbModule;
import io.jooby.trpc.avaje.jsonb.TrpcAvajeJsonbModule;

public class AvajeTrpcProtocolTest extends AbstractTrpcProtocolTest {
  @Override
  protected void installJsonEngine(Jooby app) {
    app.install(new AvajeJsonbModule());
    app.install(new TrpcAvajeJsonbModule());
  }
}
