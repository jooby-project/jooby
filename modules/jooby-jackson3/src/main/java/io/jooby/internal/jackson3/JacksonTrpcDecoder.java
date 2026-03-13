/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import io.jooby.rpc.trpc.TrpcDecoder;
import tools.jackson.databind.ObjectReader;

public class JacksonTrpcDecoder<T> implements TrpcDecoder<T> {

  final ObjectReader reader;

  public JacksonTrpcDecoder(ObjectReader reader) {
    this.reader = reader;
  }

  @Override
  public T decode(String name, byte[] payload) {
    return reader.readValue(payload);
  }

  @Override
  public T decode(String name, String payload) {
    return reader.readValue(payload);
  }
}
