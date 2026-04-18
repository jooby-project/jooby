/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson;

import java.lang.reflect.Type;

import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.SneakyThrows;
import io.jooby.json.JsonCodec;

public class JacksonJsonCodec implements JsonCodec {
  private final ObjectMapper mapper;

  public JacksonJsonCodec(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public <T> T decode(@NonNull String json, @NonNull Type type) {
    try {
      return mapper.readValue(json, mapper.getTypeFactory().constructType(type));
    } catch (JsonProcessingException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  @Override
  public <T> T decode(@NonNull String json, @NonNull Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  @Override
  public @NonNull String encode(@NonNull Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw SneakyThrows.propagate(e);
    }
  }
}
