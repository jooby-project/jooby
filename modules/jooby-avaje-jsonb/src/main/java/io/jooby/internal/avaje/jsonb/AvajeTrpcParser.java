/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import java.lang.reflect.Type;

import io.avaje.jsonb.Jsonb;
import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcParser;
import io.jooby.trpc.TrpcReader;

public class AvajeTrpcParser implements TrpcParser {

  private final Jsonb jsonb;

  public AvajeTrpcParser(Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  @Override
  public <T> TrpcDecoder<T> decoder(Type type) {
    // Avaje resolves the AOT-generated adapter here
    return new AvajeTrpcDecoder<>(jsonb.type(type));
  }

  @Override
  public TrpcReader reader(byte[] payload, boolean isTuple) {
    return new AvajeTrpcReader(jsonb.reader(payload), isTuple);
  }

  @Override
  public TrpcReader reader(String payload, boolean isTuple) {
    return new AvajeTrpcReader(jsonb.reader(payload), isTuple);
  }
}
