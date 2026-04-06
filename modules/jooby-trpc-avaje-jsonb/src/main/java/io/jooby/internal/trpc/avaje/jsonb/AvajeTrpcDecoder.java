/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.avaje.jsonb;

import io.avaje.jsonb.JsonType;
import io.jooby.trpc.TrpcDecoder;

public class AvajeTrpcDecoder<T> implements TrpcDecoder<T> {

  final JsonType<T> typeAdapter;

  public AvajeTrpcDecoder(JsonType<T> typeAdapter) {
    this.typeAdapter = typeAdapter;
  }

  @Override
  public T decode(String name, byte[] payload) {
    return typeAdapter.fromJson(payload);
  }

  @Override
  public T decode(String name, String payload) {
    return typeAdapter.fromJson(payload);
  }
}
