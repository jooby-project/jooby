/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import java.lang.reflect.Type;

import io.jooby.trpc.TrpcDecoder;
import io.jooby.trpc.TrpcParser;
import io.jooby.trpc.TrpcReader;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

public class JacksonTrpcParser implements TrpcParser {

  private final ObjectMapper mapper;

  public JacksonTrpcParser(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public <T> TrpcDecoder<T> decoder(Type type) {
    // Resolve the type exactly once at startup
    var javaType = mapper.constructType(type);
    // FIX: Tell Jackson not to panic when it sees the ']' or ',' immediately following the object
    // in the stream.
    var reader = mapper.readerFor(javaType).without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    return new JacksonTrpcDecoder<>(reader);
  }

  @Override
  public TrpcReader reader(byte[] payload, boolean isTuple) {
    return new JacksonTrpcReader(mapper.createParser(payload), isTuple);
  }

  @Override
  public TrpcReader reader(String payload, boolean isTuple) {
    return new JacksonTrpcReader(mapper.createParser(payload), isTuple);
  }
}
