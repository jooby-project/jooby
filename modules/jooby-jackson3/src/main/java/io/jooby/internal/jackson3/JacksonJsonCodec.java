/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import java.lang.reflect.Type;

import org.jspecify.annotations.NonNull;

import io.jooby.json.JsonCodec;
import tools.jackson.databind.ObjectMapper;

public class JacksonJsonCodec implements JsonCodec {
  private final ObjectMapper mapper;

  public JacksonJsonCodec(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public <T> T decode(@NonNull String json, @NonNull Type type) {
    return mapper.readValue(json, mapper.getTypeFactory().constructType(type));
  }

  @Override
  public <T> T decode(@NonNull String json, @NonNull Class<T> type) {
    return mapper.readValue(json, type);
  }

  @Override
  public @NonNull String encode(@NonNull Object value) {
    return mapper.writeValueAsString(value);
  }
}
