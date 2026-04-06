/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc.avaje.jsonb;

import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.spi.JsonbComponent;
import io.jooby.internal.trpc.avaje.jsonb.AvajeTrpcResponseAdapter;
import io.jooby.trpc.TrpcResponse;

public class TrpcJsonbExtension implements JsonbComponent {
  @Override
  public void register(Jsonb.Builder jsonb) {
    jsonb.add(TrpcResponse.class, AvajeTrpcResponseAdapter::new);
  }
}
