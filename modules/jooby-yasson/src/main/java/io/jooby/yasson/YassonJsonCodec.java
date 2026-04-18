/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.yasson;

import java.lang.reflect.Type;

import io.jooby.json.JsonCodec;
import jakarta.json.bind.Jsonb;

class YassonJsonCodec implements JsonCodec {
  private final Jsonb jsonb;

  public YassonJsonCodec(Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  @Override
  public <T> T decode(String json, Type type) {
    return jsonb.fromJson(json, type);
  }

  @Override
  public String encode(Object value) {
    return jsonb.toJson(value);
  }
}
