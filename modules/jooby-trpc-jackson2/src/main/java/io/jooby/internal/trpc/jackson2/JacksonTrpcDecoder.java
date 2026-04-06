/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectReader;
import io.jooby.SneakyThrows;
import io.jooby.trpc.TrpcDecoder;

public class JacksonTrpcDecoder<T> implements TrpcDecoder<T> {

  final ObjectReader reader;

  public JacksonTrpcDecoder(ObjectReader reader) {
    this.reader = reader;
  }

  @Override
  public T decode(String name, byte[] payload) {
    try {
      return reader.readValue(payload);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public T decode(String name, String payload) {
    try {
      return reader.readValue(payload);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
