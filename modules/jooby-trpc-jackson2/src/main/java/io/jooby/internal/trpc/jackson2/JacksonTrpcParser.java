/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.jackson2;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.SneakyThrows;
import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcParser;
import io.jooby.trpc.TrpcReader;

public class JacksonTrpcParser implements TrpcParser {

  private final ObjectMapper mapper;

  public JacksonTrpcParser(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public <T> TrpcDecoder<T> decoder(Type type) {
    // Resolve the type exactly once at startup
    var javaType = mapper.constructType(type);
    var reader = mapper.readerFor(javaType).without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    return new JacksonTrpcDecoder<>(reader);
  }

  @Override
  public TrpcReader reader(byte[] payload, boolean isTuple) {
    try {
      return new JacksonTrpcReader(mapper.createParser(payload), isTuple);
    } catch (IOException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  @Override
  public TrpcReader reader(String payload, boolean isTuple) {
    try {
      return new JacksonTrpcReader(mapper.createParser(payload), isTuple);
    } catch (IOException e) {
      throw SneakyThrows.propagate(e);
    }
  }
}
