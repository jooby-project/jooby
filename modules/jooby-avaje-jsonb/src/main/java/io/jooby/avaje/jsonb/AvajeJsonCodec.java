/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.jsonb;

import java.lang.reflect.Type;

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.jooby.json.JsonCodec;

class AvajeJsonCodec implements JsonCodec {
  private final Jsonb jsonb;

  public AvajeJsonCodec(Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T decode(String json, Type type) {
    var jsonType = (JsonType<T>) jsonb.type(type);
    return jsonType.fromJson(json);
  }

  @Override
  public String encode(Object value) {
    return jsonb.toJson(value);
  }
}
